package com.linbit.drbdmanage.security;

import static com.linbit.drbdmanage.security.AccessType.CHANGE;
import static com.linbit.drbdmanage.security.AccessType.CONTROL;
import static com.linbit.drbdmanage.security.AccessType.USE;
import static com.linbit.drbdmanage.security.AccessType.VIEW;
import static com.linbit.drbdmanage.security.Privilege.PRIVILEGE_LIST;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.linbit.InvalidNameException;
import com.linbit.drbdmanage.security.AccessContext;
import com.linbit.drbdmanage.security.AccessDeniedException;
import com.linbit.drbdmanage.security.AccessType;
import com.linbit.drbdmanage.security.Identity;
import com.linbit.drbdmanage.security.IdentityName;
import com.linbit.drbdmanage.security.ObjectProtection;
import com.linbit.drbdmanage.security.Privilege;
import com.linbit.drbdmanage.security.PrivilegeSet;
import com.linbit.drbdmanage.security.Role;
import com.linbit.drbdmanage.security.RoleName;
import com.linbit.drbdmanage.security.SecTypeName;
import com.linbit.drbdmanage.security.SecurityLevel;
import com.linbit.drbdmanage.security.SecurityType;
import com.linbit.drbdmanage.stateflags.Flags;
import com.linbit.drbdmanage.stateflags.StateFlagsBits;
import com.linbit.testutils.SimpleIterator;

public class StateFlagBitsTest
{
    private AccessContext sysCtx;
    private AccessContext rootCtx;
    private PrivilegeSet privSysAll;

    private Identity userId;
    private Identity someOtherUserId;

    private Role userRole;
    private Role someOtherRole;

    private SecurityType userSecDomain;
    private SecurityType someOtherUserSecDomain;

    @Before
    public void setUp() throws InvalidNameException, AccessDeniedException
    {
        sysCtx = new AccessContext(
            new Identity(new IdentityName("SYSTEM")),
            new Role(new RoleName("SYSTEM")),
            new SecurityType(new SecTypeName("SYSTEM")),
            new PrivilegeSet(Privilege.PRIV_SYS_ALL)
        );
        rootCtx = sysCtx.clone();
        rootCtx.privEffective.enablePrivileges(PRIVILEGE_LIST);
        privSysAll = new PrivilegeSet(Privilege.PRIV_SYS_ALL.id);

        userId = new Identity(new IdentityName("User"));
        someOtherUserId = new Identity(new IdentityName("SomeOtherUser"));

        userRole = new Role(new RoleName("UserRole"));
        someOtherRole = new Role(new RoleName("SomeOtherRole"));

        userSecDomain = new SecurityType(new SecTypeName("UserSecType"));
        someOtherUserSecDomain = new SecurityType(new SecTypeName("SomeOtherUserSecType"));

        SecurityLevel.set(rootCtx, SecurityLevel.MAC);
    }

    @Test
    public void testEnableAllFlags() throws AccessDeniedException
    {
        SimpleIterator iterator = new SimpleIterator(new Object[][]
        {
            // objProt acl entry for user
            { null, VIEW, USE, CHANGE, CONTROL },

            // preset flags for stateflags
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_ONE },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_TWO },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_THREE },
        });

        for (Object[] iteration : iterator)
        {
            AccessType grantedAt = (AccessType) iteration[0];
            ObjectProtection objProt = createObjectProtection(grantedAt);
            FlagImpl[] preSetFlags = asFlagImplArray(iteration, 1, 2, 3);
            StateFlagBitsImpl stateFlags = createStateflags(objProt, preSetFlags);

            AccessContext accCtx = createUserDefaultAccessContext();

            long preSet = asLong(preSetFlags);

            if (grantedAt != null && grantedAt.hasAccess(CHANGE))
            {
                stateFlags.enableAllFlags(accCtx);
                assertEquals(FlagImpl.getValidMask(), stateFlags.getFlagsBits(rootCtx));
            }
            else
            {
                try
                {
                    stateFlags.enableAllFlags(accCtx);
                    fail("Exception expected");
                }
                catch (AccessDeniedException expected)
                {
                    // expected
                }
                assertEquals(preSet, stateFlags.getFlagsBits(rootCtx));
            }
        }
    }

    @Test
    public void testDisableAllFlags() throws AccessDeniedException
    {
        SimpleIterator iterator = new SimpleIterator(new Object[][]
        {
            // objProt acl entry for user
            { null, VIEW, USE, CHANGE, CONTROL },

            // preset flags for stateflags
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_ONE },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_TWO },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_THREE },
        });

        for (Object[] iteration : iterator)
        {
            AccessType grantedAt = (AccessType) iteration[0];
            FlagImpl[] preSetFlags = asFlagImplArray(iteration, 1, 2, 3);

            ObjectProtection objProt = createObjectProtection(grantedAt);
            StateFlagBitsImpl stateFlags = createStateflags(objProt, preSetFlags);

            AccessContext accCtx = createUserDefaultAccessContext();

            long preSet = asLong(preSetFlags);

            if (grantedAt != null && grantedAt.hasAccess(CHANGE))
            {
                stateFlags.disableAllFlags(accCtx);
                assertEquals(0, stateFlags.getFlagsBits(rootCtx));
            }
            else
            {
                try
                {
                    stateFlags.disableAllFlags(accCtx);
                    fail("Exception expected");
                }
                catch (AccessDeniedException expected)
                {
                    // expected
                }
                assertEquals(preSet, stateFlags.getFlagsBits(rootCtx));
            }
        }
    }

    @Test
    public void testEnableFlags() throws AccessDeniedException
    {
        SimpleIterator iterator = new SimpleIterator(new Object[][]
        {
            // objProt acl entry for user
            { null, VIEW, USE, CHANGE, CONTROL },

            // preset flags for stateflags
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_ONE },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_TWO },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_THREE },

            // flags we will try to enable
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_ONE },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_TWO },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_THREE },
        });

        for (Object[] iteration : iterator)
        {
            AccessType grantedAt = (AccessType) iteration[0];
            FlagImpl[] preSetFlags = asFlagImplArray(iteration, 1, 2, 3);
            FlagImpl[] flagsToSet = asFlagImplArray(iteration, 4, 5, 6);

            ObjectProtection objProt = createObjectProtection(grantedAt);
            StateFlagBitsImpl stateFlags = createStateflags(objProt, preSetFlags);

            AccessContext accCtx = createUserDefaultAccessContext();

            long preSet = asLong(preSetFlags);
            long toEnableSet = asLong(flagsToSet);

            if (grantedAt != null && grantedAt.hasAccess(CHANGE))
            {
                stateFlags.enableFlags(accCtx, flagsToSet);
                assertEquals(toEnableSet | preSet, stateFlags.getFlagsBits(rootCtx));
            }
            else
            {
                try
                {
                    stateFlags.enableFlags(accCtx);
                    fail("Exception expected");
                }
                catch (AccessDeniedException expected)
                {
                    // expected
                }
                assertEquals(preSet, stateFlags.getFlagsBits(rootCtx));
            }
        }
    }

    @Test
    public void testDisableFlags() throws AccessDeniedException
    {
        SimpleIterator iterator = new SimpleIterator(new Object[][]
        {
            // objProt acl entry for user
            { null, VIEW, USE, CHANGE, CONTROL },

            // preset flags for stateflags
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_ONE },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_TWO },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_THREE },

            // flags we will try to disable
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_ONE },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_TWO },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_THREE },
        });

        for (Object[] iteration : iterator)
        {
            AccessType grantedAt = (AccessType) iteration[0];
            FlagImpl[] preSetFlags = asFlagImplArray(iteration, 1, 2, 3);
            FlagImpl[] flagsToUnset = asFlagImplArray(iteration, 4, 5, 6);

            ObjectProtection objProt = createObjectProtection(grantedAt);
            StateFlagBitsImpl stateFlags = createStateflags(objProt, preSetFlags);

            AccessContext accCtx = createUserDefaultAccessContext();

            long preSet = asLong(preSetFlags);
            long toDisableSet = asLong(flagsToUnset);

            if (grantedAt != null && grantedAt.hasAccess(CHANGE))
            {
                stateFlags.disableFlags(accCtx, flagsToUnset);
                assertEquals(preSet & ~toDisableSet, stateFlags.getFlagsBits(rootCtx));
            }
            else
            {
                try
                {
                    stateFlags.disableFlags(accCtx);
                    fail("Exception expected");
                }
                catch (AccessDeniedException expected)
                {
                    // expected
                }
                assertEquals(preSet, stateFlags.getFlagsBits(rootCtx));
            }
        }
    }

    @Test
    public void testEnableFlagsExcept() throws AccessDeniedException
    {
        SimpleIterator iterator = new SimpleIterator(new Object[][]
        {
            // objProt acl entry for user
            { null, VIEW, USE, CHANGE, CONTROL },

            // preset flags for stateflags
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_ONE },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_TWO },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_THREE },

            // flags we will try to enableExcept
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_ONE },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_TWO },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_THREE },
        });

        for (Object[] iteration : iterator)
        {
            AccessType grantedAt = (AccessType) iteration[0];
            FlagImpl[] preSetFlags = asFlagImplArray(iteration, 1, 2, 3);
            FlagImpl[] flagsToSetExcept = asFlagImplArray(iteration, 4, 5, 6);

            ObjectProtection objProt = createObjectProtection(grantedAt);
            StateFlagBitsImpl stateFlags = createStateflags(objProt, preSetFlags);

            AccessContext accCtx = createUserDefaultAccessContext();

            long preSet = asLong(preSetFlags);
            long toEnableExceptSet = asLong(flagsToSetExcept);

            if (grantedAt != null && grantedAt.hasAccess(CHANGE))
            {
                stateFlags.enableFlagsExcept(accCtx, flagsToSetExcept);
                assertEquals(preSet | (FlagImpl.getValidMask() & ~toEnableExceptSet),
                    stateFlags.getFlagsBits(rootCtx));
            }
            else
            {
                try
                {
                    stateFlags.enableFlagsExcept(accCtx);
                    fail("Exception expected");
                }
                catch (AccessDeniedException expected)
                {
                    // expected
                }
                assertEquals(preSet, stateFlags.getFlagsBits(rootCtx));
            }
        }
    }

    @Test
    public void testDisableFlagsExcept() throws AccessDeniedException
    {
        SimpleIterator iterator = new SimpleIterator(new Object[][]
        {
            // objProt acl entry for user
            { null, VIEW, USE, CHANGE, CONTROL },

            // preset flags for stateflags
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_ONE },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_TWO },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_THREE },

            // flags we will try to disableExcept
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_ONE },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_TWO },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_THREE },
        });

        for (Object[] iteration : iterator)
        {
            AccessType grantedAt = (AccessType) iteration[0];
            FlagImpl[] preSetFlags = asFlagImplArray(iteration, 1, 2, 3);
            FlagImpl[] flagsToUnset = asFlagImplArray(iteration, 4, 5, 6);

            ObjectProtection objProt = createObjectProtection(grantedAt);
            StateFlagBitsImpl stateFlags = createStateflags(objProt, preSetFlags);

            AccessContext accCtx = createUserDefaultAccessContext();

            long preSet = asLong(preSetFlags);
            long toDisableSet = asLong(flagsToUnset);

            if (grantedAt != null && grantedAt.hasAccess(CHANGE))
            {
                stateFlags.disableFlagsExcept(accCtx, flagsToUnset);
                assertEquals(preSet & toDisableSet, stateFlags.getFlagsBits(rootCtx));
            }
            else
            {
                try
                {
                    stateFlags.disableFlagsExcept(accCtx);
                    fail("Exception expected");
                }
                catch (AccessDeniedException expected)
                {
                    // expected
                }
                assertEquals(preSet, stateFlags.getFlagsBits(rootCtx));
            }
        }
    }

    @Test
    public void testIsSet() throws AccessDeniedException
    {
        SimpleIterator iterator = new SimpleIterator(new Object[][]
        {
            // objProt acl entry for user
            { null, VIEW, USE, CHANGE, CONTROL },

            // preset flags for stateflags
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_ONE },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_TWO },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_THREE },

            // flags we will call isSet with
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_ONE },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_TWO },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_THREE },
        });

        for (Object[] iteration : iterator)
        {
            AccessType grantedAt = (AccessType) iteration[0];
            FlagImpl[] preSetFlags = asFlagImplArray(iteration, 1, 2, 3);
            FlagImpl[] flagsIsSet = asFlagImplArray(iteration, 4, 5, 6);

            ObjectProtection objProt = createObjectProtection(grantedAt);
            StateFlagBitsImpl stateFlags = createStateflags(objProt, preSetFlags);

            AccessContext accCtx = createUserDefaultAccessContext();

            long preSet = asLong(preSetFlags);
            long isSet = asLong(flagsIsSet);

            if (grantedAt != null && grantedAt.hasAccess(VIEW))
            {
                assertEquals((preSet & isSet) == isSet, stateFlags.isSet(accCtx, flagsIsSet));
            }
            else
            {
                try
                {
                    stateFlags.isSet(accCtx);
                    fail("Exception expected");
                }
                catch (AccessDeniedException expected)
                {
                    // expected
                }
            }
        }
    }

    @Test
    public void testIsUnset() throws AccessDeniedException
    {
        SimpleIterator iterator = new SimpleIterator(new Object[][]
        {
            // objProt acl entry for user
            { null, VIEW, USE, CHANGE, CONTROL },

            // preset flags for stateflags
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_ONE },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_TWO },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_THREE },

            // flags we will call isUnset with
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_ONE },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_TWO },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_THREE },
        });

        for (Object[] iteration : iterator)
        {
            AccessType grantedAt = (AccessType) iteration[0];
            FlagImpl[] preSetFlags = asFlagImplArray(iteration, 1, 2, 3);
            FlagImpl[] flagsIsUnset = asFlagImplArray(iteration, 4, 5, 6);

            ObjectProtection objProt = createObjectProtection(grantedAt);
            StateFlagBitsImpl stateFlags = createStateflags(objProt, preSetFlags);

            AccessContext accCtx = createUserDefaultAccessContext();

            long preSet = asLong(preSetFlags);
            long isUnset = asLong(flagsIsUnset);

            if (grantedAt != null && grantedAt.hasAccess(VIEW))
            {
                assertEquals((preSet & isUnset) == 0, stateFlags.isUnset(accCtx, flagsIsUnset));
            }
            else
            {
                try
                {
                    stateFlags.isUnset(accCtx);
                    fail("Exception expected");
                }
                catch (AccessDeniedException expected)
                {
                    // expected
                }
            }
        }
    }

    @Test
    public void testIsSomeSet() throws AccessDeniedException
    {
        SimpleIterator iterator = new SimpleIterator(new Object[][]
        {
            // objProt acl entry for user
            { null, VIEW, USE, CHANGE, CONTROL },

            // preset flags for stateflags
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_ONE },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_TWO },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_THREE },

            // flags we will call isSomeSet with
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_ONE },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_TWO },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_THREE },
        });

        for (Object[] iteration : iterator)
        {
            AccessType grantedAt = (AccessType) iteration[0];
            FlagImpl[] preSetFlags = asFlagImplArray(iteration, 1, 2, 3);
            FlagImpl[] flagsIsSomeSet = asFlagImplArray(iteration, 4, 5, 6);

            ObjectProtection objProt = createObjectProtection(grantedAt);
            StateFlagBitsImpl stateFlags = createStateflags(objProt, preSetFlags);

            AccessContext accCtx = createUserDefaultAccessContext();

            long preSet = asLong(preSetFlags);
            long isSomeSet = asLong(flagsIsSomeSet);

            if (grantedAt != null && grantedAt.hasAccess(VIEW))
            {
                assertEquals((preSet & isSomeSet) != 0, stateFlags.isSomeSet(accCtx, flagsIsSomeSet));
            }
            else
            {
                try
                {
                    stateFlags.isSomeSet(accCtx);
                    fail("Exception expected");
                }
                catch (AccessDeniedException expected)
                {
                    // expected
                }
            }
        }
    }

    @Test
    public void testIsSomeUnset() throws AccessDeniedException
    {
        SimpleIterator iterator = new SimpleIterator(new Object[][]
        {
            // objProt acl entry for user
            { null, VIEW, USE, CHANGE, CONTROL },

            // preset flags for stateflags
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_ONE },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_TWO },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_THREE },

            // flags we will call isSomeUnset with
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_ONE },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_TWO },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_THREE },
        });

        for (Object[] iteration : iterator)
        {
            AccessType grantedAt = (AccessType) iteration[0];
            FlagImpl[] preSetFlags = asFlagImplArray(iteration, 1, 2, 3);
            FlagImpl[] flagsIsSomeUnset = asFlagImplArray(iteration, 4, 5, 6);

            ObjectProtection objProt = createObjectProtection(grantedAt);
            StateFlagBitsImpl stateFlags = createStateflags(objProt, preSetFlags);

            AccessContext accCtx = createUserDefaultAccessContext();

            long preSet = asLong(preSetFlags);
            long isSomeUnset = asLong(flagsIsSomeUnset);

            if (grantedAt != null && grantedAt.hasAccess(VIEW))
            {
                assertEquals((preSet & isSomeUnset) != isSomeUnset, stateFlags.isSomeUnset(accCtx, flagsIsSomeUnset));
            }
            else
            {
                try
                {
                    stateFlags.isSomeUnset(accCtx);
                    fail("Exception expected");
                }
                catch (AccessDeniedException expected)
                {
                    // expected
                }
            }
        }
    }

    @Test
    public void testgetBitFlags() throws AccessDeniedException
    {
        SimpleIterator iterator = new SimpleIterator(new Object[][]
        {
            // objProt acl entry for user
            { null, VIEW, USE, CHANGE, CONTROL },

            // preset flags for stateflags
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_ONE },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_TWO },
            { FlagImpl.FLAG_ZERO, FlagImpl.FLAG_THREE },
        });

        for (Object[] iteration : iterator)
        {
            AccessType grantedAt = (AccessType) iteration[0];
            FlagImpl[] preSetFlags = asFlagImplArray(iteration, 1, 2, 3);

            ObjectProtection objProt = createObjectProtection(grantedAt);
            StateFlagBitsImpl stateFlags = createStateflags(objProt, preSetFlags);

            AccessContext accCtx = createUserDefaultAccessContext();

            long preSet = asLong(preSetFlags);

            if (grantedAt != null && grantedAt.hasAccess(VIEW))
            {
                assertEquals(preSet, stateFlags.getFlagsBits(accCtx));
            }
            else
            {
                try
                {
                    stateFlags.getFlagsBits(accCtx);
                    fail("Exception expected");
                }
                catch (AccessDeniedException expected)
                {
                    // expected
                }
            }
        }
    }

    @Test
    public void testGetValidFlagsBits() throws AccessDeniedException
    {
        AccessType[] grantedAccessTypes = new AccessType[]
        {
            null, VIEW, USE, CHANGE, CONTROL
        };
        for (AccessType grantedAt : grantedAccessTypes)
        {
            ObjectProtection objProt = createObjectProtection(grantedAt);
            AccessContext accCtx = createUserDefaultAccessContext();

            long expectedMask = 0;
            StateFlagBitsImpl stateFlags = new StateFlagBitsImpl(objProt, 0);

            boolean expectException = grantedAt == null || !grantedAt.hasAccess(VIEW);
            if (expectException)
            {
                try
                {
                    stateFlags.getFlagsBits(accCtx);
                }
                catch (AccessDeniedException expected)
                {
                    // expected
                }
            }
            else
            {
                assertEquals(0, stateFlags.getFlagsBits(accCtx));
                for (int bitsSet = 0; bitsSet < 64; ++bitsSet)
                {
                    expectedMask |= 1L << bitsSet;
                    stateFlags = new StateFlagBitsImpl(objProt, expectedMask);
                    assertEquals(expectedMask, stateFlags.getValidFlagsBits(accCtx));
                }
            }

        }
    }

    private AccessContext createUserDefaultAccessContext() throws AccessDeniedException
    {
        AccessContext accCtx = new AccessContext(userId, userRole, userSecDomain, new PrivilegeSet(Privilege.PRIV_MAC_OVRD));
        accCtx.privEffective.enablePrivileges(Privilege.PRIV_MAC_OVRD);
        return accCtx;
    }

    private ObjectProtection createObjectProtection(AccessType... grantedAccess) throws AccessDeniedException
    {
        AccessContext objCtx = new AccessContext(someOtherUserId, someOtherRole, someOtherUserSecDomain, privSysAll);
        ObjectProtection objProt = new ObjectProtection(objCtx);
        for (AccessType grantedAt : grantedAccess)
        {
            if (grantedAt != null)
            {
                objProt.addAclEntry(rootCtx, userRole, grantedAt);
            }
        }
        return objProt;
    }

    private FlagImpl[] asFlagImplArray(Object[] iteration, int... iterationIdx)
    {
        FlagImpl[] flags = new FlagImpl[iterationIdx.length];
        for (int i = 0; i < iterationIdx.length; ++i)
        {
            flags[i] = (FlagImpl) iteration[iterationIdx[i]];
        }
        return flags;
    }

    private StateFlagBitsImpl createStateflags(ObjectProtection op, FlagImpl... flagsPreSet) throws AccessDeniedException
    {
        StateFlagBitsImpl stateFlags = new StateFlagBitsImpl(op, FlagImpl.getValidMask());
        stateFlags.enableFlags(rootCtx, flagsPreSet);
        return stateFlags;
    }

    private long asLong(FlagImpl... flags)
    {
        long ret = 0;
        for (FlagImpl flag : flags)
        {
            ret |= flag.value;
        }
        return ret;
    }

    private static enum FlagImpl implements Flags
    {
        FLAG_ZERO(0L), FLAG_ONE(1L), FLAG_TWO(2L), FLAG_THREE(4L);

        private long value;

        private FlagImpl(long value)
        {
            this.value = value;
        }

        @Override
        public long getFlagValue()
        {
            return value;
        }

        public static long getValidMask()
        {
            long mask = 0;
            for (FlagImpl flag : FlagImpl.values())
            {
                mask |= flag.value;
            }
            return mask;
        }
    }

    private class StateFlagBitsImpl extends StateFlagsBits<FlagImpl>
    {
        public StateFlagBitsImpl(ObjectProtection objProtRef, long validFlagsMask)
        {
            super(objProtRef, validFlagsMask);
        }
    }
}
