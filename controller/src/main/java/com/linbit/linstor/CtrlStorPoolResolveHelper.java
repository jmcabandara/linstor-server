package com.linbit.linstor;

import com.linbit.ImplementationError;
import com.linbit.linstor.annotation.ApiContext;
import com.linbit.linstor.annotation.PeerContext;
import com.linbit.linstor.api.ApiCallRcImpl;
import com.linbit.linstor.api.ApiCallRcWith;
import com.linbit.linstor.api.ApiConsts;
import com.linbit.linstor.core.apicallhandler.controller.CtrlPropsHelper;
import com.linbit.linstor.core.apicallhandler.response.ApiRcException;
import com.linbit.linstor.core.identifier.NodeName;
import com.linbit.linstor.core.objects.Resource;
import com.linbit.linstor.core.objects.StorPool;
import com.linbit.linstor.core.objects.Volume;
import com.linbit.linstor.core.objects.VolumeDefinition;
import com.linbit.linstor.layer.resource.CtrlRscLayerDataFactory;
import com.linbit.linstor.propscon.InvalidKeyException;
import com.linbit.linstor.propscon.Props;
import com.linbit.linstor.security.AccessContext;
import com.linbit.linstor.security.AccessDeniedException;
import com.linbit.linstor.storage.kinds.DeviceProviderKind;
import com.linbit.linstor.utils.layer.LayerVlmUtils;

import static com.linbit.linstor.api.ApiConsts.FAIL_INVLD_STOR_POOL_NAME;
import static com.linbit.linstor.api.ApiConsts.FAIL_NOT_FOUND_DFLT_STOR_POOL;
import static com.linbit.linstor.api.ApiConsts.KEY_STOR_POOL_NAME;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.util.stream.Collectors;

@Singleton
public class CtrlStorPoolResolveHelper
{
    private final AccessContext apiCtx;
    private final CtrlPropsHelper ctrlPropsHelper;
    private final Provider<AccessContext> peerCtxProvider;
    private final Provider<CtrlRscLayerDataFactory> layerStackHelper;

    @Inject
    public CtrlStorPoolResolveHelper(
        @ApiContext AccessContext apiCtxRef,
        @PeerContext Provider<AccessContext> peerCtxProviderRef,
        CtrlPropsHelper ctrlPropsHelperRef,
        Provider<CtrlRscLayerDataFactory> layerStackHelperRef
    )
    {
        apiCtx = apiCtxRef;
        peerCtxProvider = peerCtxProviderRef;
        ctrlPropsHelper = ctrlPropsHelperRef;
        layerStackHelper = layerStackHelperRef;
    }

    /**
     * Resolves the correct storage pool and also handles error/warnings in diskless modes.
     */
    public ApiCallRcWith<StorPool> resolveStorPool(
        Resource rsc,
        VolumeDefinition vlmDfn,
        boolean isRscDiskless
    )
    {
        return resolveStorPool(peerCtxProvider.get(), rsc, vlmDfn, isRscDiskless, true);
    }

    /**
     * Resolves the correct storage pool and also handles error/warnings in diskless modes.
     */
    public ApiCallRcWith<StorPool> resolveStorPool(
        Resource rsc,
        VolumeDefinition vlmDfn,
        boolean isRscDiskless,
        boolean throwExcIfStorPoolIsNull
    )
    {
        return resolveStorPool(peerCtxProvider.get(), rsc, vlmDfn, isRscDiskless, throwExcIfStorPoolIsNull);
    }

    /**
     * Resolves the correct storage pool and also handles error/warnings in diskless modes.
     */
    public ApiCallRcWith<StorPool> resolveStorPool(
        AccessContext accCtx,
        Resource rsc,
        VolumeDefinition vlmDfn,
        boolean isRscDiskless,
        boolean throwExcIfStorPoolIsNull
    )
    {
        ApiCallRcImpl responses = new ApiCallRcImpl();

        StorPool storPool;
        try
        {
            Props rscProps = ctrlPropsHelper.getProps(accCtx, rsc);
            Props vlmDfnProps = ctrlPropsHelper.getProps(accCtx, vlmDfn);
            Props rscDfnProps = ctrlPropsHelper.getProps(accCtx, rsc.getDefinition());
            Props rscGrpProps = ctrlPropsHelper.getProps(accCtx, rsc.getDefinition().getResourceGroup());
            Props nodeProps = ctrlPropsHelper.getProps(accCtx, rsc.getNode());

            PriorityProps vlmPrioProps = new PriorityProps(
                rscProps, vlmDfnProps, rscDfnProps, rscGrpProps, nodeProps
            );

            String storPoolNameStr = vlmPrioProps.getProp(KEY_STOR_POOL_NAME);
            if (isRscDiskless)
            {
                if (storPoolNameStr == null || "".equals(storPoolNameStr))
                {
                    // If the resource was marked as diskless then there should be a resource property identifying the
                    // diskless pool.
                    storPool = null;
                }
                else
                {
                    storPool = rsc.getNode().getStorPool(
                        apiCtx,
                        LinstorParsingUtils.asStorPoolName(storPoolNameStr)
                    );
                }

                checkBackingDiskWithDiskless(rsc, storPool);
            }
            else
            {
                if (storPoolNameStr == null || "".equals(storPoolNameStr))
                {
                    storPoolNameStr = InternalApiConsts.DEFAULT_STOR_POOL_NAME;
                }
                storPool = rsc.getNode().getStorPool(
                    apiCtx,
                    LinstorParsingUtils.asStorPoolName(storPoolNameStr)
                );

                if (storPool != null)
                {
                    if (storPool.getDeviceProviderKind().hasBackingDevice())
                    {
                        // If the storage pool has backing storage, check that it is of the same kind as the peers
                        checkSameKindAsPeers(vlmDfn, rsc.getNode().getName(), storPool);
                    }
                }
            }

            if (throwExcIfStorPoolIsNull)
            {
                checkStorPoolLoaded(rsc, storPool, storPoolNameStr, vlmDfn);
            }
        }
        catch (InvalidKeyException | AccessDeniedException exc)
        {
            throw new ImplementationError(exc);
        }

        return new ApiCallRcWith<>(responses, storPool);
    }

    private void checkSameKindAsPeers(VolumeDefinition vlmDfn, NodeName nodeName, StorPool storPool)
        throws AccessDeniedException
    {
        DeviceProviderKind driverKind = storPool.getDeviceProviderKind();

        for (Resource peerRsc : vlmDfn.getResourceDefinition().streamResource(apiCtx).collect(Collectors.toList()))
        {
            boolean isDiskless = peerRsc.isDrbdDiskless(apiCtx) || peerRsc.isNvmeInitiator(apiCtx);
            if (!isDiskless && !peerRsc.getNode().getName().equals(nodeName))
            {
                Volume peerVlm = peerRsc.getVolume(vlmDfn.getVolumeNumber());
                if (peerVlm != null)
                {
                    for (StorPool peerStorPool : LayerVlmUtils.getStorPoolSet(peerVlm, apiCtx, false))
                    {
                        DeviceProviderKind peerKind = peerStorPool.getDeviceProviderKind();
                        if (!DeviceProviderKind.isMixingAllowed(driverKind, peerKind))
                        {
                            throw new ApiRcException(makeInvalidDriverKindError(driverKind, peerKind));
                        }
                    }
                }
            }
        }
    }

    private void checkBackingDiskWithDiskless(final Resource rsc, final StorPool storPool)
    {
        if (storPool != null && storPool.getDeviceProviderKind().hasBackingDevice())
        {
            throw new ApiRcException(ApiCallRcImpl
                .entryBuilder(FAIL_INVLD_STOR_POOL_NAME,
                              "Storage pool with backing disk not allowed with diskless resource.")
                .setCause(String.format("Resource '%s' flagged as diskless, but a storage pool '%s' " +
                        "with backing disk was specified.",
                    rsc.getDefinition().getName().displayValue,
                    storPool.getName().displayValue))
                .setCorrection("Use a storage pool with a diskless driver or remove the diskless flag.")
                .build(),
                new LinStorException("Incorrect storage pool used.")
            );
        }
    }

    private void checkStorPoolLoaded(
        final Resource rsc,
        StorPool storPool,
        String storPoolNameStr,
        final VolumeDefinition vlmDfn
    )
    {
        if (storPool == null)
        {
            throw new ApiRcException(ApiCallRcImpl
                .entryBuilder(FAIL_NOT_FOUND_DFLT_STOR_POOL, "The storage pool '" + storPoolNameStr + "' " +
                    "for resource '" + rsc.getDefinition().getName().displayValue + "' " +
                    "for volume number '" + vlmDfn.getVolumeNumber().value + "' " +
                    "is not deployed on node '" + rsc.getNode().getName().displayValue + "'.")
                .setDetails("The resource which should be deployed had at least one volume definition " +
                    "(volume number '" + vlmDfn.getVolumeNumber().value + "') which LinStor " +
                    "tried to automatically create. " +
                    "The storage pool name for this new volume was looked for in order in " +
                    "the properties of the resource, volume definition, resource definition and node, " +
                    "and finally in a system wide default storage pool name defined by " +
                    "the LinStor controller.")
                .build(),
                new LinStorException("Dependency not found")
            );
        }
    }

    private ApiCallRcImpl.ApiCallRcEntry makeInvalidDriverKindError(
        DeviceProviderKind driverKind,
        DeviceProviderKind peerKind
    )
    {
        return ApiCallRcImpl
            .entryBuilder(
                ApiConsts.FAIL_INVLD_STOR_DRIVER,
                String.format("Storage driver '%s' not allowed for volume.", driverKind)
            )
            .setDetails("Using storage pools with different storage drivers on the same volume definition " +
                "is not supported.")
            .setCorrection(String.format("Use a storage pool with the driver '%s'", peerKind))
            .build();
    }
}
