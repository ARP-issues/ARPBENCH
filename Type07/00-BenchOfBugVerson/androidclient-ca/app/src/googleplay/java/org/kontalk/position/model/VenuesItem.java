/*
 * Kontalk Android client
 * Copyright (C) 2017 Kontalk Devteam <devteam@kontalk.org>

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.kontalk.position.model;

import java.util.List;

public class VenuesItem {
    private boolean hasPerk;
    private Specials specials;
    private String referralId;
    private List<Object> venueChains;
    private boolean verified;
    private String url;
    private BeenHere beenHere;
    private HereNow hereNow;
    private boolean venueRatingBlacklisted;
    private Stats stats;
    private Contact contact;
    private String name;
    private Location location;
    private String id;
    private List<CategoriesItem> categories;

    public boolean isHasPerk() {
        return hasPerk;
    }

    public void setHasPerk(boolean hasPerk) {
        this.hasPerk = hasPerk;
    }

    public Specials getSpecials() {
        return specials;
    }

    public void setSpecials(Specials specials) {
        this.specials = specials;
    }

    public String getReferralId() {
        return referralId;
    }

    public void setReferralId(String referralId) {
        this.referralId = referralId;
    }

    public List<Object> getVenueChains() {
        return venueChains;
    }

    public void setVenueChains(List<Object> venueChains) {
        this.venueChains = venueChains;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public BeenHere getBeenHere() {
        return beenHere;
    }

    public void setBeenHere(BeenHere beenHere) {
        this.beenHere = beenHere;
    }

    public HereNow getHereNow() {
        return hereNow;
    }

    public void setHereNow(HereNow hereNow) {
        this.hereNow = hereNow;
    }

    public boolean isVenueRatingBlacklisted() {
        return venueRatingBlacklisted;
    }

    public void setVenueRatingBlacklisted(boolean venueRatingBlacklisted) {
        this.venueRatingBlacklisted = venueRatingBlacklisted;
    }

    public Stats getStats() {
        return stats;
    }

    public void setStats(Stats stats) {
        this.stats = stats;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<CategoriesItem> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoriesItem> categories) {
        this.categories = categories;
    }

    @Override
    public String toString() {
        return
            "VenuesItem{" +
                "hasPerk = '" + hasPerk + '\'' +
                ",specials = '" + specials + '\'' +
                ",referralId = '" + referralId + '\'' +
                ",venueChains = '" + venueChains + '\'' +
                ",verified = '" + verified + '\'' +
                ",url = '" + url + '\'' +
                ",beenHere = '" + beenHere + '\'' +
                ",hereNow = '" + hereNow + '\'' +
                ",venueRatingBlacklisted = '" + venueRatingBlacklisted + '\'' +
                ",stats = '" + stats + '\'' +
                ",contact = '" + contact + '\'' +
                ",name = '" + name + '\'' +
                ",location = '" + location + '\'' +
                ",id = '" + id + '\'' +
                ",categories = '" + categories + '\'' +
                "}";
    }
}
