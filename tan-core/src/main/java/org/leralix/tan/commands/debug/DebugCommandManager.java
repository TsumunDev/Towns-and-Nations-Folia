package org.leralix.tan.commands.debug;
import org.leralix.lib.commands.CommandManager;
import org.leralix.lib.commands.MainHelpCommand;
import org.leralix.tan.integration.nexo.NexoCheckCommand;
import org.leralix.tan.integration.nexo.NexoDebugCommand;
public class DebugCommandManager extends CommandManager {
  public DebugCommandManager() {
    super("tan.debug.commands");
    addSubCommand(new SaveData());
    addSubCommand(new CreateBackup());
    addSubCommand(new LogLevelCommand());
    addSubCommand(new NexoCheckCommand());
    addSubCommand(new NexoDebugCommand());
    addSubCommand(new SkipDay());
    addSubCommand(new PlaySound());
    addSubCommand(new MainHelpCommand(this));
    addSubCommand(new SendReport());
  }
  @Override
  public String getName() {
    return "tandebug";
  }
}