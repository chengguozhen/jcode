/*
 * Copyright © 2017 My Company and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.future.network.noti.pub;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.broadcast.rev170826.BreakingNewsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.broadcast.rev170826.WeatherForecastBuilder;

import java.time.LocalDate;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by 10032272 on 2017/8/26 0026.
 */
public class Radio {
    NotificationPublishService publishService;

    private Timer timer = new Timer();
    private Timer timer1 = new Timer();
    public Radio(NotificationPublishService publishService) {
        this.publishService = publishService;
        broadcast();
    }

    private void broadcast() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                WeatherForecastBuilder weatherForecastBuilder = new WeatherForecastBuilder();
                weatherForecastBuilder.setDate(LocalDate.now().toString());
                weatherForecastBuilder.setLocation("Nan Jing City");
                weatherForecastBuilder.setWeather("The wind is coming,The rain is coming,Frogs are coming with drums!");
                try {
                    publishService.putNotification(weatherForecastBuilder.build());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        },3000,5000);

        timer1.schedule(new TimerTask() {
            @Override
            public void run() {
                BreakingNewsBuilder breakingNewsBuilder = new BreakingNewsBuilder();
                breakingNewsBuilder.setDesc("Planets will hit the Earth in 2 days");
                ListenableFuture f = publishService.offerNotification(breakingNewsBuilder.build());

            }
        },15000,60000);
    }

    public void close() {
        timer.cancel();
        timer1.cancel();
    }
}
