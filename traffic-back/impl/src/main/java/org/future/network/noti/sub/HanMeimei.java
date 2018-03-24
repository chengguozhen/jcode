/*
 * Copyright © 2017 My Company and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.future.network.noti.sub;

import org.opendaylight.yang.gen.v1.urn.opendaylight.broadcast.rev170826.BreakingNews;
import org.opendaylight.yang.gen.v1.urn.opendaylight.broadcast.rev170826.BroadcastListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.broadcast.rev170826.WeatherForecast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by 10032272 on 2017/8/26 0026.
 */
public class HanMeimei implements BroadcastListener {
    Logger LOG = LoggerFactory.getLogger(HanMeimei.class);
    @Override
    public void onBreakingNews(BreakingNews notification) {
        LOG.info("HanMeimei:"+notification.getDesc()+",Feel hopeless!!!!!!!!\n");
    }

    @Override
    public void onWeatherForecast(WeatherForecast notification) {
        LOG.info("Hanmeimei:Date="+notification.getDate()+",Loaction="
                +notification.getLocation()+",Weather="
                +notification.getWeather());
        LOG.info("Feel sad!!!\n");
    }
}
