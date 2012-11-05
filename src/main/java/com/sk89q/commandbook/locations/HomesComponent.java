/*
 * CommandBook
 * Copyright (C) 2011 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.commandbook.locations;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.commands.PaginatedResult;
import com.sk89q.commandbook.util.LocationUtil;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.commandbook.util.TeleportPlayerIterator;
import com.sk89q.minecraft.util.commands.*;
import com.zachsthings.libcomponents.ComponentInformation;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@ComponentInformation(friendlyName = "Homes", desc = "Home management system")
public class HomesComponent extends LocationsComponent {
    public HomesComponent() {
        super("Home");
    }

    public void enable() {
        super.enable();
        registerCommands(Commands.class);
    }

    public class Commands {
        @Command(aliases = {"home"}, usage = "[world] [target] [owner] [home]", desc = "Teleport to a home", min = 0, max = 4)
        @CommandPermissions({"commandbook.home.teleport"})
        public void home(CommandContext args, CommandSender sender) throws CommandException {
            Iterable<Player> targets = null;
            NamedLocation home = null;
            Location loc;

            // Detect arguments based on the number of arguments provided
            if (args.argsLength() == 0) {
                Player player = PlayerUtil.checkPlayer(sender);
                targets = PlayerUtil.matchPlayers(player);
                home = getManager().get(player.getWorld(), player.getName());
            } else if (args.argsLength() == 1) {
                Player player = PlayerUtil.checkPlayer(sender);
                targets = PlayerUtil.matchPlayers(player);
                try {
                    home = getManager().get(player.getWorld(), PlayerUtil.matchPlayerExactly(player, args.getString(0)).getName());
                } catch (CommandException e) {
                    home = getManager().get(player.getWorld(), player.getName() + "@" + args.getString(0));
                }
            } else if (args.argsLength() == 2) {
                Player player = PlayerUtil.checkPlayer(sender);
                targets = PlayerUtil.matchPlayers(player);
                home = getManager().get(player.getWorld(), PlayerUtil.matchPlayerExactly(player, args.getString(0)).getName() + "@" + args.getString(1));
            } else if (args.argsLength() == 3) {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));
                if (getManager().isPerWorld()) {
                    Player player = PlayerUtil.checkPlayer(sender);
                    home = getManager().get(player.getWorld(), args.getString(1));
                } else {
                    home = getManager().get(null, args.getString(1));
                }

                // Check permissions!
                for (Player target : targets) {
                    if (target != sender) {
                        CommandBook.inst().checkPermission(sender, "commandbook.home.teleport.other");
                        break;
                    }
                }
            } else if (args.argsLength() == 4) {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));
                if (getManager().isPerWorld()) {
                    Player player = PlayerUtil.checkPlayer(sender);
                    home = getManager().get(player.getWorld(), args.getString(1) + "@" + args.getString(3));
                } else {
                    home = getManager().get(null, args.getString(1) + "@" + args.getString(3));
                }

                // Check permissions!
                for (Player target : targets) {
                    if (target != sender) {
                        CommandBook.inst().checkPermission(sender, "commandbook.home.teleport.other");
                        break;
                    }
                }
            }

            if (home != null) {
                if (!home.getCreatorName().equals(sender.getName())) {
                    CommandBook.inst().checkPermission(sender, "commandbook.home.other");
                }
                loc = home.getLocation();
            } else {
                throw new CommandException("A home for the given player does not exist.");
            }

            (new TeleportPlayerIterator(sender, loc)).iterate(targets);
        }

        @Command(aliases = {"sethome"}, usage = "[owner] [location]", desc = "Set a home", min = 0, max = 3)
        @CommandPermissions({"commandbook.home.set"})
        public void setHome(CommandContext args, CommandSender sender) throws CommandException {
            String homeName;
            Location loc;
            Player player = null;

            // Detect arguments based on the number of arguments provided
            if (args.argsLength() == 0) {
                player = PlayerUtil.checkPlayer(sender);
                homeName = player.getName();
                loc = player.getLocation();
            } else if (args.argsLength() == 1) {
                try {
                	homeName = PlayerUtil.matchPlayerExactly(sender, args.getString(0)).getName();
                } catch (CommandException e) {
                    homeName = sender.getName() + "@" + args.getString(0);
                }
                player = PlayerUtil.checkPlayer(sender);
                loc = player.getLocation();

                // Check permissions!
                if (!homeName.equals(sender.getName())) {
                    CommandBook.inst().checkPermission(sender, "commandbook.home.set.other");
                }
            } else if (args.argsLength() == 2) {
                homeName = args.getString(1);
                loc = LocationUtil.matchLocation(sender, args.getString(0));

                // Check permissions!
                if (!homeName.equals(sender.getName())) {
                    CommandBook.inst().checkPermission(sender, "commandbook.home.set.other");
                }
            } else {
                try {
                	homeName = PlayerUtil.matchPlayerExactly(sender, args.getString(1)).getName();
                } catch (CommandException e) {
                    homeName = sender.getName() + "@" + args.getString(1);
                }
                loc = LocationUtil.matchLocation(sender, args.getString(0));

                // Check permissions!
                if (!homeName.equals(sender.getName())) {
                    CommandBook.inst().checkPermission(sender, "commandbook.home.set.other");
                }
            }

            getManager().create(homeName, loc, player);

            sender.sendMessage(ChatColor.YELLOW + "Home set.");
        }

        @Command(aliases = {"homes"}, desc = "Home management")
        @NestedCommand({ManagementCommands.class})
        public void homes() throws CommandException {
        }
    }

    public class ManagementCommands {
        @Command(aliases = {"del", "delete", "remove", "rem"},
                usage = "[name] [world]", desc = "Remove a home", min = 0, max = 2 )
        @CommandPermissions({"commandbook.home.remove"})
        public void removeCmd(CommandContext args, CommandSender sender) throws CommandException {
            World world;
            String name = sender.getName();
            if (args.argsLength() == 2) {
                world = LocationUtil.matchWorld(sender, args.getString(1));
            } else {
                world = PlayerUtil.checkPlayer(sender).getWorld();
            }
            if (args.argsLength() > 0) name = args.getString(0);
            remove(name, world, sender);
        }

        @Command(aliases = {"list", "show"}, usage = "[-w world] [page]", desc = "List homes",
                flags = "w:", min = 0, max = 1 )
        @CommandPermissions({"commandbook.home.list"})
        public void listCmd(CommandContext args, CommandSender sender) throws CommandException {
            list(args, sender);
        }
    }

    @Override
    public PaginatedResult<NamedLocation> getListResult() {
        final String defaultWorld = CommandBook.server().getWorlds().get(0).getName();
        return new PaginatedResult<NamedLocation>("Owner - World  - Location") {
            @Override
            public String format(NamedLocation entry) {
                return entry.getCreatorName()
                        + " - " + (entry.getWorldName() == null ? defaultWorld : entry.getWorldName())
                        + " - " + entry.getLocation().getBlockX() + "," + entry.getLocation().getBlockY()
                        + "," + entry.getLocation().getBlockZ();
            }
        };
    }
}
