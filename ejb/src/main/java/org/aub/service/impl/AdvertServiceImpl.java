package org.aub.service.impl;

import org.apache.commons.io.IOUtils;
import org.aub.db.dao.AdvertDao;
import org.aub.db.domain.Advert;
import org.aub.db.domain.AdvertProfile;
import org.aub.mongodb.odm.exception.PersistenceException;
import org.aub.service.AdvertService;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Stateless
public class AdvertServiceImpl implements AdvertService {

    private static final String WEB_SITE_ENCODING = "UTF-8";
    private static final String SEARCH_URL_PAGE_PARAM = "[page]";

    @Inject
    private AdvertDao advertDao;

    @Override
    public List<Advert> getNewAdverts(AdvertProfile profile) {
        List<Advert> adverts = new ArrayList<>();
        //TODO throw errors to the client side
        long searchPagesNumber = profile.getSearchPagesNumber();
        String searchUrl = profile.getSearchUrl();
        for (long i = 1; i <= searchPagesNumber; i++) {
            try {
                String url = searchUrl.replace(SEARCH_URL_PAGE_PARAM, String.valueOf(i));
                InputStream in = new URL(url).openStream();
                try {
                    String page = IOUtils.toString(in, WEB_SITE_ENCODING);
                    adverts.addAll(getAdvertsBySourcePage(page, profile));
                } finally {
                    IOUtils.closeQuietly(in);
                }
            } catch (Exception e) {
            }
        }
        return adverts;
    }


    private List<Advert> getAdvertsBySourcePage(String page, AdvertProfile profile) {
        List<Advert> adverts = new ArrayList<>();
        Pattern titlePattern = Pattern.compile(profile.getAdvertPattern());
        Matcher matcher = titlePattern.matcher(page);
        while (matcher.find()) {
            Advert currentAdvert = new Advert();
            currentAdvert.setAdvertProfileId(profile.getId());
            String adUrl = matcher.group();
            if(!adUrl.contains(profile.getSiteUrl())) {
                adUrl = profile.getSiteUrl() + adUrl;
            }
            currentAdvert.setUrl(adUrl);
            currentAdvert.setCreatedDate(new Date());
            try {
                advertDao.create(currentAdvert);
                adverts.add(currentAdvert);
            } catch (PersistenceException e) {
                e.printStackTrace();
            }
        }

        return adverts;
    }
}
