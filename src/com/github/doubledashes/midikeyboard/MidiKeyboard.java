package com.github.doubledashes.midikeyboard;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sound.midi.*;

public class MidiKeyboard extends JavaPlugin implements Receiver {

    private MidiDevice.Info[] deviceInfos;
    private Instrument instrument;
    private int octaveOffset;

    private Player musician;
    private MidiDevice device;

    @Override
    public void onEnable() {
        super.onEnable();

        this.deviceInfos = MidiSystem.getMidiDeviceInfo();
        this.instrument = Instrument.PIANO;
        this.octaveOffset = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();

        if (device != null && device.isOpen())
            this.device.close();
    }

    @Override
    public void send(MidiMessage message, long l) {
        if (message instanceof ShortMessage) {
            ShortMessage sm = (ShortMessage) message;
            int channel = sm.getChannel();

            int command = sm.getCommand();
            if (command == ShortMessage.NOTE_ON) {

                int key = sm.getData1();
                int velocity = sm.getData2();

                Note note = createNote(key);
                if (note != null) {
                    Location location = musician.getLocation();
                    this.musician.playNote(location, instrument, note);
                }

            } else if (command == ShortMessage.NOTE_OFF) {

//                int key = sm.getData1();
//                int velocity = sm.getData2();
//
//                Note note = createNote(key);
//                System.out.println("OFF: " + note);

            } else
                System.out.println("Command:" + command);
        }
    }

    @Override
    public void close() {
    }

    private Note createNote(int key) {
        int note = key % Note.Tone.TONES_COUNT;
        int octave = this.octaveOffset + ((key / Note.Tone.TONES_COUNT) - 1);

        if (octave < 0 || octave > 1) {
            System.out.println("(ignored) KEY " + key + " (note: " + note + ", octave: " + octave + ")");
            return null;
        }

        System.out.println("KEY " + key + " (note: " + note + ", octave: " + octave + ")");
        //if(octave != 2 || tone == Note.Tone.F && sharped)


        switch (note) {
            case 0:
                return new Note(octave, Note.Tone.C, false);
            case 1:
                return new Note(octave, Note.Tone.C, true);
            case 2:
                return new Note(octave, Note.Tone.D, false);
            case 3:
                return new Note(octave, Note.Tone.D, true);
            case 4:
                return new Note(octave, Note.Tone.E, false);
            case 5:
                return new Note(octave, Note.Tone.F, false);
            case 6:
                return new Note(octave, Note.Tone.F, true);
            case 7:
                return new Note(octave, Note.Tone.G, false);
            case 8:
                return new Note(octave, Note.Tone.G, true);
            case 9:
                return new Note(octave, Note.Tone.A, false);
            case 10:
                return new Note(octave, Note.Tone.A, true);
            case 11:
                return new Note(octave, Note.Tone.B, false);
            default:
                throw new IllegalArgumentException("Unknown note " + note);
        }
    }

    private void setDevice(int index) throws MidiUnavailableException {
        MidiDevice.Info deviceInfo = deviceInfos[index];

        if (this.device != null)
            this.device.close();
        this.device = MidiSystem.getMidiDevice(deviceInfo);
        this.device.open();

        Transmitter transmitter = device.getTransmitter();
        transmitter.setReceiver(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (label.equals("midi") || label.equals("midikeyboard:midi")) {
            if (args.length < 1)
                return false;

            String function = args[0];
            if (function.equals("device")) {
                return executeDeviceCommand(sender, args);
            } else if (function.equals("config")) {
                return executeConfigCommand(sender, args);
            }
        }

        return false;
    }

    private boolean executeDeviceCommand(CommandSender sender, String[] args) {
        if (args.length < 2)
            return false;

        String action = args[1];
        if (action.equals("list")) {

            ComponentBuilder builder = new ComponentBuilder();
            builder.append("MIDI Devices:\n");

            this.deviceInfos = MidiSystem.getMidiDeviceInfo();
            for (int i = 0; i < deviceInfos.length; i++) {
                MidiDevice.Info info = deviceInfos[i];

                String name = info.getName();
                String description = info.getDescription();
                builder.append(name + " (" + description + ")\n")
                        .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/midi device set " + i)
                        );

            }

            sender.spigot().sendMessage(builder.create());

            return true;

        } else if (action.equals("set")) {
            if (args.length < 3)
                return false;

            try {

                int index = Integer.parseInt(args[2]);
                setDevice(index);

                sender.sendMessage("Updated device: " + deviceInfos[index]);

            } catch (NumberFormatException | MidiUnavailableException e) {
                sender.sendMessage(e.toString());
                return false;
            }

            if (sender instanceof Player)
                this.musician = (Player) sender;

            return true;

        } else
            return false;
    }

    private boolean executeConfigCommand(CommandSender sender, String[] args) {
        if (args.length < 2)
            return false;

        String action = args[1];
        if (action.equals("instrument")) {
            if (args.length < 3)
                return false;

            String argument = args[2];
            this.instrument = Instrument.valueOf(argument);
            sender.sendMessage("Updated instrument: " + instrument);

            return true;

        } else if (action.equals("octave")) {
            if (args.length < 3)
                return false;

            try {
                this.octaveOffset = Integer.parseInt(args[2]);
                sender.sendMessage("Updated octave offset: " + octaveOffset);
            } catch (NumberFormatException e) {
                sender.sendMessage(e.toString());
                return false;
            }

            return true;
        } else
            return false;
    }

}
