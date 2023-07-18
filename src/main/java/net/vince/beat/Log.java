package net.vince.beat;

import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Log extends ListenerAdapter {

  @Override
  public void onGenericEvent(GenericEvent event) {
    System.out.println(event);
  }

}
