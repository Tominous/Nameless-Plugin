package com.namelessmc.namelessplugin.sponge.commands;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.spongepowered.api.Game;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.namelessmc.namelessplugin.sponge.NamelessPlugin;

/*
 *  Report CMD by IsS127 (Sponge'd by Lmmb74)
 */
    
public class ReportCommand implements CommandExecutor {

	@Inject
	Game game;
	String permission;

	@Override
	public CommandResult execute(CommandSource sender, CommandContext args) throws CommandException {
		// check if player has permission Permission
		if(sender.hasPermission(permission + ".report")){
			// check if hasSetUrl
			if(NamelessPlugin.getInstance().hasSetUrl == false){
				sender.sendMessage(Text.of(TextColors.RED, "Please set an API Url in the configuration!"));
				return CommandResult.success();
			}

			// Ensure user who inputted command is player and not console
			if(sender instanceof Player){
				Player player = (Player) sender;

				// Try to register user
				game.getScheduler().createAsyncExecutor(new Runnable(){
					@Override
					public void run(){
						// Ensure email is set
						if(args.getOne("player").get().toString().length() < 2){
							player.sendMessage(Text.of(TextColors.RED, "Incorrect usage: /report username reason"));
							return;
						}

						// Send POST request to API
						try {

							// Initialise strings
							String toPostReported;
							String toPostReporter;

							// Initialise JSON response + parser
							JsonObject response = new JsonObject();
							JsonParser parser = new JsonParser();

							String uuid = "";

							// Try to get the user being reported
							Player reported = game.getServer().getPlayer(args.getOne("player").get().toString()).get();
							if(!reported.isOnline()){
								// User is offline, get UUID from username
								HttpURLConnection lookupConnection = (HttpURLConnection) new URL("https://api.mojang.com/users/profiles/minecraft/" + reported.getName()).openConnection();

								// Handle response
								BufferedReader streamReader = new BufferedReader(new InputStreamReader(lookupConnection.getInputStream(), "UTF-8"));
								StringBuilder lookupResponseBuilder = new StringBuilder();

								String lookupResponseString;
								while((lookupResponseString = streamReader.readLine()) != null)
									lookupResponseBuilder.append(lookupResponseString);

								if(lookupResponseBuilder.toString() == null || parser.parse(lookupResponseBuilder.toString()) == null){
									player.sendMessage(Text.of("Unable to submit report, please try again later."));
									return; // Unable to find user from username
								}

								response = parser.parse(lookupResponseBuilder.toString()).getAsJsonObject();

								uuid = response.get("id").getAsString();

								if(uuid == null){
									player.sendMessage(Text.of("Unable to submit report, please try again later."));
									return; // Unable to find user from username
								}

								toPostReported =  "reported_username=" + URLEncoder.encode(reported.getName(), "UTF-8") + "&reported_uuid=" + URLEncoder.encode(uuid, "UTF-8");

							} else {
								toPostReported =  "reported_username=" + URLEncoder.encode(reported.getName(), "UTF-8") + "&reported_uuid=" + URLEncoder.encode(reported.getUniqueId().toString(), "UTF-8");
							}

							// Get report content
							String content = args.getOne("reason").get().toString();
							// Add reporter info + report content to post content
							toPostReporter =  "&reporter_uuid=" + URLEncoder.encode(player.getUniqueId().toString(), "UTF-8")
											  + "&content=" + URLEncoder.encode(content, "UTF-8");

							String toPostString = toPostReported + toPostReporter;

							// Initialise API connection
							URL apiConnection = new URL(NamelessPlugin.getInstance().getAPIUrl() + "/createReport");

							HttpURLConnection connection = (HttpURLConnection) apiConnection.openConnection();
							connection.setRequestMethod("POST");
							connection.setRequestProperty("Content-Length", Integer.toString(toPostString.length()));
							connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
							connection.setDoOutput(true);
							connection.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");

							// Initialise output stream
							DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());

							// Write request
							outputStream.writeBytes(toPostString);

							// Initialise input stream
							InputStream inputStream = connection.getInputStream();

							// Handle response
							BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
							StringBuilder responseBuilder = new StringBuilder();

							String responseString;
							while((responseString = streamReader.readLine()) != null)
								responseBuilder.append(responseString);

							response = parser.parse(responseBuilder.toString()).getAsJsonObject();

							if(response.has("error")){
								// Error with request
								player.sendMessage(Text.of(TextColors.RED, "Error: " + response.get("message").getAsString()));
							} else {
								// Display success message to user
								player.sendMessage(Text.of(TextColors.GREEN, response.get("message").getAsString()));
							}

							// Close output/input stream
							outputStream.flush();
							outputStream.close();
							inputStream.close();

							// Disconnect
							connection.disconnect();

						} catch(Exception e){
							// Exception
							sender.sendMessage(Text.of(TextColors.RED, "There was an unknown error whilst executing the command."));
							e.printStackTrace();
						}
					}
				});

			} else {
				// User must be ingame to use register command
				sender.sendMessage(Text.of("You must be ingame to use this command."));
			}

		} else {
			sender.sendMessage(Text.of(TextColors.RED, "You don't have permission to this command!"));
		}

		return CommandResult.success();
	}
}