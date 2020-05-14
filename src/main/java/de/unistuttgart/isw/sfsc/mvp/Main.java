package de.unistuttgart.isw.sfsc.mvp;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class Main {

  public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
    ChannelFactory.main(args);
    ChannelClient.main(args);
  }
}
