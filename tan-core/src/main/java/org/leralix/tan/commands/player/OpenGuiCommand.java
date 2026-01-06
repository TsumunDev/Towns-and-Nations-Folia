package org.leralix.tan.commands.player;
import java.util.Collections;
import java.util.List;
import org.bukkit.entity.Player;
import org.leralix.lib.commands.PlayerSubCommand;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.gui.user.MainMenu;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.FoliaScheduler;
import org.leralix.tan.utils.text.TanChatUtils;
public class OpenGuiCommand extends PlayerSubCommand {
  @Override
  public String getName() {
    return "gui";
  }
  @Override
  public String getDescription() {
    return Lang.TOWN_GUI_COMMAND_DESC.getDefault();
  }
  public int getArguments() {
    return 2;
  }
  @Override
  public String getSyntax() {
    return "/ccn gui";
  }
  @Override
  public List<String> getTabCompleteSuggestions(Player player, String lowerCase, String[] args) {
    return Collections.emptyList();
  }
  @Override
  public void perform(Player player, String[] args) {
    if (args.length == 1) {
      MainMenu.open(player);
    } else if (args.length > 1) {
      PlayerDataStorage.getInstance()
          .get(player)
          .thenAccept(
              tanPlayer -> {
                FoliaScheduler.runTask(
                    TownsAndNations.getPlugin(),
                    () -> {
                      LangType lang = tanPlayer.getLang();
                      TanChatUtils.message(player, Lang.TOO_MANY_ARGS_ERROR.get(lang));
                      TanChatUtils.message(
                          player, Lang.CORRECT_SYNTAX_INFO.get(lang, getSyntax()));
                    });
              });
    }
  }
}