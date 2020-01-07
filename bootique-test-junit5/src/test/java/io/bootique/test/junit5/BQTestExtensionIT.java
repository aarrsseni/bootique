package io.bootique.test.junit5;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.inject.Inject;
import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.cli.Cli;
import io.bootique.command.CommandOutcome;
import io.bootique.command.CommandWithMetadata;
import io.bootique.config.ConfigurationFactory;
import io.bootique.log.BootLogger;
import io.bootique.meta.application.CommandMetadata;
import io.bootique.test.TestIO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BQTestExtensionIT {

    @RegisterExtension
    public static BQTestExtension bqTestExtension = new BQTestExtension();

    @Test
    public void testCreateRuntimeInjection() {
        BQRuntime runtime = bqTestExtension.app("-x").autoLoadModules().createRuntime();
        Assertions.assertArrayEquals(new String[]{"-x"}, runtime.getArgs());
    }

    @Test
    public void testCreateRuntime_Streams_NoTrace() {

        TestIO io = TestIO.noTrace();

        CommandOutcome result = bqTestExtension.app("-x")
                .autoLoadModules()
                .module(b -> BQCoreModule.extend(b).addCommand(XCommand.class))
                .bootLogger(io.getBootLogger())
                .createRuntime()
                .run();

        assertTrue(result.isSuccess());
        assertEquals("--out--", io.getStdout().trim());
        assertEquals("--err--", io.getStderr().trim());
    }

    @Test
    public void testConfigEnvExcludes_System() {
        System.setProperty("bq.a", "bq_a");
        System.setProperty("bq.c.m.k", "bq_c_m_k");
        System.setProperty("bq.c.m.l", "bq_c_m_l");

        BQRuntime runtime = bqTestExtension.app("--config=src/test/resources/configEnvironment.yml").createRuntime();

        Bean1 b1 = runtime.getInstance(ConfigurationFactory.class).config(Bean1.class, "");

        assertEquals("e", b1.a);
        assertEquals("q", b1.c.m.k);
        assertEquals("n", b1.c.m.l);

        System.clearProperty("bq.a");
        System.clearProperty("bq.c.m.k");
        System.clearProperty("bq.c.m.l");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Bean1 {
        private String a;
        private Bean2 c;

        public void setA(String a) {
            this.a = a;
        }

        public void setC(Bean2 c) {
            this.c = c;
        }
    }

    static class Bean2 {

        private Bean3 m;

        public void setM(Bean3 m) {
            this.m = m;
        }
    }

    static class Bean3 {
        private String k;
        private String f;
        private String l;

        public void setK(String k) {
            this.k = k;
        }

        public void setF(String f) {
            this.f = f;
        }

        public void setL(String l) {
            this.l = l;
        }
    }

    public static class XCommand extends CommandWithMetadata {

        private BootLogger logger;

        @Inject
        public XCommand(BootLogger logger) {
            super(CommandMetadata.builder(XCommand.class));
            this.logger = logger;
        }

        @Override
        public CommandOutcome run(Cli cli) {
            logger.stderr("--err--");
            logger.stdout("--out--");
            return CommandOutcome.succeeded();
        }
    }

}
