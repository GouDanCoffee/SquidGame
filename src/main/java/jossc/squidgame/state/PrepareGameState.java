package jossc.squidgame.state;

import cn.nukkit.plugin.PluginBase;
import java.time.Duration;
import jossc.game.state.GameState;
import org.jetbrains.annotations.NotNull;

public class PrepareGameState extends GameState {

  public PrepareGameState(PluginBase plugin) {
    super(plugin);
  }

  @NotNull
  @Override
  public Duration getDuration() {
    return Duration.ZERO;
  }

  @Override
  protected void onStart() {}

  @Override
  public void onUpdate() {}

  @Override
  protected void onEnd() {}
}
