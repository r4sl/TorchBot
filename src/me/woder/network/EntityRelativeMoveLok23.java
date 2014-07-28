package me.woder.network;

import java.io.IOException;

import me.woder.bot.Client;
import me.woder.bot.Entity;
import me.woder.world.Location;

public class EntityRelativeMoveLok23 extends Packet{
    public EntityRelativeMoveLok23(Client c) {
        super(c);
    }
    
    @Override
    public void read(Client c, int len) throws IOException{
       int eid = c.in.readInt();
       byte x = c.in.readByte();
       byte y = c.in.readByte();
       byte z = c.in.readByte();
       byte yaw = c.in.readByte();
       byte pitch = c.in.readByte();
       Entity e = c.en.findEntityId(eid);
       if(e != null){
          e.sx += x;
          e.sy += y;
          e.sz += z;          
          e.setLocationLook(new Location(c.whandle.getWorld(), e.sx/32.0D, e.sy/32.0D, e.sz/32.0D), yaw, pitch);
       }
    }

}