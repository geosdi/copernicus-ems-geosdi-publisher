/**
 MIT License

 Copyright (c) 2017 geoSDI

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */
package org.geosdi.copernicus.ems.publisher;

import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Francesco Izzi - CNR IMAA geoSDI Group
 * @email francesco.izzi@geosdi.org
 */

@SpringBootApplication
public class PublisherApplication implements CommandLineRunner{

    @Autowired
    private EMSPublisherService emsService;

    private static final Logger log = LoggerFactory.getLogger(PublisherApplication.class);


    @Override
    public void run(String... args) throws MalformedURLException {
        GeoServerRESTReader reader = new GeoServerRESTReader(emsService.getGeoserverRestURL(), emsService.getGeoserverRestUser(), emsService.getGeoserverRestPassword());
        GeoServerRESTPublisher publisher = new GeoServerRESTPublisher(emsService.getGeoserverRestURL(), emsService.getGeoserverRestUser(), emsService.getGeoserverRestPassword());


        log.info("MissionID: " +emsService.getMissionID());

        String url = "http://emergency.copernicus.eu/mapping/list-of-components/"+emsService.getMissionID();

        log.info("Fetching... " + url);

        Document doc = null;
        try {
            doc = Jsoup.connect(url).get();

        Elements links = doc.select("a[href]");

        log.info("\nLinks found : ", links.size());
        log.info("\nInpsect for ZIP file : ");

        for (Element link : links) {
            if( trim(link.text(), 35).equals("ZIP")) {
                emsService.downloadAndPublishToGeoServer(link.attr("abs:href"), reader, publisher);
            }
        }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

	public static void main(String[] args) throws IOException {
	    log.info("Starting up ...");

        SpringApplication.run(PublisherApplication.class, args);
	}

	private static String trim(String s, int width) {
		if (s.length() > width)
			return s.substring(0, width-1) + ".";
		else
			return s;
	}


}
