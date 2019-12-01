package xyz.fireslime.gapless_audio_loop;

import android.media.MediaPlayer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

class GaplessPlayer {
    private MediaPlayer currentPlayer = null;
    private MediaPlayer nextPlayer = null;

    private String url;
    private double volumeleft;
    private double volumeright;
    private boolean stopplaying;

    GaplessPlayer(String url, double volumeleft, double  volumeright) {
        this.url = url;
        this.volumeleft = volumeleft;
        this.volumeright = volumeright;
        this.stopplaying = false;

        try {
            currentPlayer = new MediaPlayer();
            currentPlayer.setDataSource(url);
            currentPlayer.setVolume((float) volumeleft, (float) volumeright);
            currentPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    currentPlayer.start();
                }
            });
            currentPlayer.prepareAsync();
            createNextMediaPlayer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setVolume(double volumeleft, double volumeright) {
        this.volumeleft = volumeleft;
        this.volumeright = volumeright;

        if (currentPlayer != null) {
            currentPlayer.setVolume((float) volumeleft, (float) volumeright);
        }
        if (nextPlayer != null) {
            nextPlayer.setVolume((float) volumeleft, (float) volumeright);
        }
    }

    private void createNextMediaPlayer() {
        if (this.stopplaying){
            stop();
            return;
        }
        nextPlayer = new MediaPlayer();
        try {
            nextPlayer.setDataSource(url);
            nextPlayer.setVolume((float) volumeleft, (float) volumeright);
            nextPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    nextPlayer.seekTo(0);
                    currentPlayer.setNextMediaPlayer(nextPlayer);
                    currentPlayer.setOnCompletionListener(onCompletionListener);
                }
            });
            nextPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final MediaPlayer.OnCompletionListener onCompletionListener =
            new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    currentPlayer = nextPlayer;
                    createNextMediaPlayer();
                    mediaPlayer.release();
                }
            };

    public void stop() {
        this.stopplaying = true;
        currentPlayer.stop();
        if (nextPlayer != null) {
            nextPlayer.stop();
        }
    }

    public void pause() {
        this.stopplaying = true;
        currentPlayer.pause();
        if (nextPlayer != null) {
            nextPlayer.pause();
        }
    }

    public void resume() {
        this.stopplaying = false;
        currentPlayer.start();
    }

    public void seek(int position) {
        currentPlayer.seekTo(position);
    }
}

/**
 * GaplessAudioLoopPlugin
 */
public class GaplessAudioLoopPlugin implements MethodCallHandler {
    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "gapless_audio_loop");
        channel.setMethodCallHandler(new GaplessAudioLoopPlugin());
    }

    private static int id = 0;
    private Map<Integer, GaplessPlayer> players = new HashMap<>();

    GaplessPlayer getPlayer(MethodCall call) {
        int playerId = call.argument("playerId");
        return players.get(playerId);
    }
    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("play")) {
            int id = GaplessAudioLoopPlugin.id;

            String url = call.argument("url");
            double volumeleft = call.argument("volumeleft");
            double volumeright = call.argument("volumeright");

            GaplessPlayer player = new GaplessPlayer(url, volumeleft, volumeright);
            players.put(id, player);

            GaplessAudioLoopPlugin.id++;

            result.success(id);
        } else if (call.method.equals("stop")) {
            int playerId = call.argument("playerId");
            GaplessPlayer player = players.get(playerId);

            if (player != null) {
                player.stop();
                players.remove(playerId);
            }
        } else if (call.method.equals("pause")) {
            GaplessPlayer player = getPlayer(call);

            if (player != null) {
                player.pause();
            }
        } else if (call.method.equals("resume")) {
            GaplessPlayer player = getPlayer(call);

            if (player != null) {
                player.resume();
            }
        } else if (call.method.equals("setVolume")) {
            GaplessPlayer player = getPlayer(call);
            double volumeleft = call.argument("volumeleft");
            double volumeright = call.argument("volumeright");

            if (player != null) {
                player.setVolume(volumeleft, volumeright);
            }
        } else if (call.method.equals("seek")) {
            GaplessPlayer player = getPlayer(call);
            int position = call.argument("position");

            player.seek(position);
        } else {
            result.notImplemented();
        }
    }
}
