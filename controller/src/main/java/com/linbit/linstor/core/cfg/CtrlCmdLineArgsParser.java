package com.linbit.linstor.core.cfg;

import com.linbit.linstor.InternalApiConsts;
import com.linbit.linstor.core.LinStor;

import java.io.File;
import java.nio.file.Paths;

import picocli.CommandLine;

class CtrlCmdLineArgsParser
{
    @CommandLine.Option(names = {"-c", "--config-directory"},
        description = "Configuration directory for the controller"
    )
    private String configurationDirectory;
    @CommandLine.Option(names = {"-d", "--debug-console"}, description = "")
    private Boolean debugConsole;
    @CommandLine.Option(
        names = {"--memory-database"},
        description = "Use a in memory database for testing. [format=dbtype;port;listenaddr]"
    )
    private String memoryDB;

    @CommandLine.Option(
        names = {"-p", "--stack-traces"},
        description = "print error stack traces on standard error"
    )
    private Boolean printStackTrace;

    @CommandLine.Option(names = {"-l", "--logs"}, description = "Path to the log directory")
    private String logDirectory;

    @CommandLine.Option(names = {"--log-level"},
        description = "The desired log level. Options: ERROR, WARN, INFO, DEBUG, TRACE")
    private String logLevel;

    @CommandLine.Option(names = {"--log-level-linstor"},
        description = "The desired log level. Options: ERROR, WARN, INFO, DEBUG, TRACE")
    private String logLevelLinstor;

    @CommandLine.Option(
        names = {"--rest-bind"},
        description = "Bind address for the REST HTTP server. e.g. 0.0.0.0:3370"
    )
    private String restBindAddress;

    @CommandLine.Option(
        names = {"--rest-bind-secure"},
        description = "Bind address for the REST HTTPS server. e.g. 0.0.0.0:3371"
    )
    private String restBindAddressSecure;

    @CommandLine.Option(names = {"-v", "--version"}, versionHelp = true, description = "Show the version number")
    private Boolean versionInfoRequested;

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    private Boolean usageHelpRequested;

    @CommandLine.Option(
        names = {"--disable-db-version-check"},
        description = "Disable database version version checks supported by Linstor")
    private Boolean disableDbVersionCheck;

    static void parseCommandLine(String[] args, CtrlConfig linstorCfgRef)
    {
        CtrlCmdLineArgsParser linArgParser = new CtrlCmdLineArgsParser();
        CommandLine cmd = new CommandLine(linArgParser);
        cmd.setCommandName("Controller");
        cmd.setOverwrittenOptionsAllowed(true);

        try
        {
            cmd.parse(args);
        }
        catch (Exception exc)
        {
            System.err.println(exc.getMessage());
            cmd.usage(System.err);
            System.exit(InternalApiConsts.EXIT_CODE_CMDLINE_ERROR);
        }

        if (cmd.isVersionHelpRequested())
        {
            System.out.println("LINSTOR Controller " + LinStor.VERSION_INFO_PROVIDER.getVersion());
            System.exit(InternalApiConsts.EXIT_CODE_SHUTDOWN);
        }

        if (cmd.isUsageHelpRequested())
        {
            cmd.usage(System.out);
            System.exit(InternalApiConsts.EXIT_CODE_SHUTDOWN);
        }

        if (linArgParser.configurationDirectory != null)
        {
            linstorCfgRef.setConfigDir(linArgParser.configurationDirectory + "/");
            File workingDir = Paths.get(linstorCfgRef.getConfigDir()).toAbsolutePath().toFile();
            if (workingDir.exists() && !workingDir.isDirectory())
            {
                System.err.println("Error: Given configuration directory is no directory");
                System.exit(InternalApiConsts.EXIT_CODE_CMDLINE_ERROR);
            }
        }

        linstorCfgRef.setLogDirectory(linArgParser.logDirectory);

        if (linArgParser.memoryDB != null)
        {
            String[] memopts = linArgParser.memoryDB.split(";");
            if (memopts.length > 0)
            {
                linstorCfgRef.setDbInMemory(memopts[0]);
            }
            if (memopts.length > 1)
            {
                // cArgs.setInMemoryDbPort(Integer.parseInt(memopts[1]));
                // deprecated
            }
            if (memopts.length > 2)
            {
                // cArgs.setInMemoryDbAddress(memopts[2]);
                // deprecated
            }
        }

        linstorCfgRef.setRestBindAddressWithPort(linArgParser.restBindAddress);

        linstorCfgRef.setRestSecureBindAddressWithPort(linArgParser.restBindAddressSecure);

        linstorCfgRef.setLogPrintStackTrace(linArgParser.printStackTrace);
        linstorCfgRef.setDebugConsoleEnable(linArgParser.debugConsole);

        linstorCfgRef.setLogLevel(linArgParser.logLevel);
        linstorCfgRef.setLogLevelLinstor(linArgParser.logLevelLinstor);

        linstorCfgRef.setDbDisableVersionCheck(linArgParser.disableDbVersionCheck);
    }

    private CtrlCmdLineArgsParser()
    {
    }
}