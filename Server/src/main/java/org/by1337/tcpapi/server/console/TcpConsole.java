package org.by1337.tcpapi.server.console;


import org.by1337.tcpapi.api.packet.impl.PacketPingRequest;
import org.by1337.tcpapi.server.Main;
import org.by1337.tcpapi.server.command.Command;
import org.by1337.tcpapi.server.command.CommandException;
import org.by1337.tcpapi.server.console.impl.PingCommand;
import org.by1337.tcpapi.server.logger.LogManager;
import org.by1337.tcpapi.server.network.Connection;
import org.by1337.tcpapi.server.network.Server;
import org.by1337.tcpapi.server.util.TimeUtil;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TcpConsole {
    private final static Logger logger = LogManager.getLogger();
    private volatile boolean isStopped;
    private final Command<Server> commands = new Command<>("root");
    private final long startAt = System.currentTimeMillis();

    public void start() {
        try {
            Terminal terminal = TerminalBuilder.builder().dumb(true).build();
            initCommands();
            this.readCommands(terminal);
        } catch (IOException var2) {
            logger.log(Level.SEVERE, "", var2);
        }
    }

    protected LineReader buildReader(LineReaderBuilder builder) {
        builder
                .appName("TcpServer")
                .variable(LineReader.HISTORY_FILE, java.nio.file.Paths.get(".console_history"))
                .completer(new ConsoleCommandCompleter(commands))
                .option(LineReader.Option.COMPLETE_IN_WORD, true);
        return builder.build();
    }

    private void readCommands(Terminal terminal) {
        LineReader reader = this.buildReader(LineReaderBuilder.builder().terminal(terminal));

        try {
            while (!isStopped && !Main.getServer().isStopped()) {
                String line;
                try {
                    line = reader.readLine("> ");
                } catch (EndOfFileException var9) {
                    continue;
                }

                if (line == null) {
                    break;
                }

                this.processInput(line);
            }
        } catch (UserInterruptException var10) {
            isStopped = true;
            Main.getInstance().stop();
        }

    }

    protected void processInput(String input) {
        String command = input.trim();
        if (!command.isEmpty()) {
            try {
                this.commands.process(Main.getServer(), input.split(" "));
            } catch (CommandException e) {
                logger.severe(e.getMessage());
            }
        }

    }

    public void addCommand(Command<Server> command) {
        if (commands.hasSubCommand(command.getCommand())) {
            throw new IllegalStateException("command already exist!");
        }
        commands.addSubCommand(command);
    }

    private void initCommands() {
        commands.addSubCommand(
                new Command<Server>("stop")
                        .executor((s, args) -> {
                            isStopped = true;
                            Main.getInstance().stop();
                        })
        );
        commands.addSubCommand(
                new Command<Server>("tps")
                        .executor((s, args) -> {
                            LogManager.getLogger().info(Main.getTicker().getTpsCounter().tps());
                        })
        );
        commands.addSubCommand(
                new Command<Server>("uptime")
                        .executor((s, args) -> {
                            LogManager.getLogger().info(TimeUtil.getFormat((int) ((System.currentTimeMillis() - startAt) / 1_000)));
                        })
        );
        commands.addSubCommand(new PingCommand());
    }

    public void setStopped(boolean stopped) {
        isStopped = stopped;
    }
}
