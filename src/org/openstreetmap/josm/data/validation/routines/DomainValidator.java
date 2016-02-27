/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openstreetmap.josm.data.validation.routines;

import java.net.IDN;
import java.util.Arrays;
import java.util.Locale;

/**
 * <p><b>Domain name</b> validation routines.</p>
 *
 * <p>
 * This validator provides methods for validating Internet domain names
 * and top-level domains.
 * </p>
 *
 * <p>Domain names are evaluated according
 * to the standards <a href="http://www.ietf.org/rfc/rfc1034.txt">RFC1034</a>,
 * section 3, and <a href="http://www.ietf.org/rfc/rfc1123.txt">RFC1123</a>,
 * section 2.1. No accommodation is provided for the specialized needs of
 * other applications; if the domain name has been URL-encoded, for example,
 * validation will fail even though the equivalent plaintext version of the
 * same name would have passed.
 * </p>
 *
 * <p>
 * Validation is also provided for top-level domains (TLDs) as defined and
 * maintained by the Internet Assigned Numbers Authority (IANA):
 * </p>
 *
 *   <ul>
 *     <li>{@link #isValidInfrastructureTld} - validates infrastructure TLDs
 *         (<code>.arpa</code>, etc.)</li>
 *     <li>{@link #isValidGenericTld} - validates generic TLDs
 *         (<code>.com, .org</code>, etc.)</li>
 *     <li>{@link #isValidCountryCodeTld} - validates country code TLDs
 *         (<code>.us, .uk, .cn</code>, etc.)</li>
 *   </ul>
 *
 * <p>
 * (<b>NOTE</b>: This class does not provide IP address lookup for domain names or
 * methods to ensure that a given domain name matches a specific IP; see
 * {@link java.net.InetAddress} for that functionality.)
 * </p>
 *
 * @version $Revision: 1725571 $
 * @since Validator 1.4
 */
public final class DomainValidator extends AbstractValidator {

    private static final int MAX_DOMAIN_LENGTH = 253;

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    // Regular expression strings for hostnames (derived from RFC2396 and RFC 1123)

    // RFC2396: domainlabel   = alphanum | alphanum *( alphanum | "-" ) alphanum
    // Max 63 characters
    private static final String DOMAIN_LABEL_REGEX = "\\p{Alnum}(?>[\\p{Alnum}-]{0,61}\\p{Alnum})?";

    // RFC2396 toplabel = alpha | alpha *( alphanum | "-" ) alphanum
    // Max 63 characters
    private static final String TOP_LABEL_REGEX = "\\p{Alpha}(?>[\\p{Alnum}-]{0,61}\\p{Alnum})?";

    // RFC2396 hostname = *( domainlabel "." ) toplabel [ "." ]
    // Note that the regex currently requires both a domain label and a top level label, whereas
    // the RFC does not. This is because the regex is used to detect if a TLD is present.
    // If the match fails, input is checked against DOMAIN_LABEL_REGEX (hostnameRegex)
    // RFC1123 sec 2.1 allows hostnames to start with a digit
    private static final String DOMAIN_NAME_REGEX =
            "^(?:" + DOMAIN_LABEL_REGEX + "\\.)+" + "(" + TOP_LABEL_REGEX + ")\\.?$";

    private final boolean allowLocal;

    /**
     * Singleton instance of this validator, which
     *  doesn't consider local addresses as valid.
     */
    private static final DomainValidator DOMAIN_VALIDATOR = new DomainValidator(false);

    /**
     * Singleton instance of this validator, which does
     *  consider local addresses valid.
     */
    private static final DomainValidator DOMAIN_VALIDATOR_WITH_LOCAL = new DomainValidator(true);

    /**
     * RegexValidator for matching domains.
     */
    private final RegexValidator domainRegex =
            new RegexValidator(DOMAIN_NAME_REGEX);
    /**
     * RegexValidator for matching a local hostname
     */
    // RFC1123 sec 2.1 allows hostnames to start with a digit
    private final RegexValidator hostnameRegex =
            new RegexValidator(DOMAIN_LABEL_REGEX);

    /**
     * Returns the singleton instance of this validator. It
     *  will not consider local addresses as valid.
     * @return the singleton instance of this validator
     */
    public static synchronized DomainValidator getInstance() {
        inUse = true;
        return DOMAIN_VALIDATOR;
    }

    /**
     * Returns the singleton instance of this validator,
     *  with local validation as required.
     * @param allowLocal Should local addresses be considered valid?
     * @return the singleton instance of this validator
     */
    public static synchronized DomainValidator getInstance(boolean allowLocal) {
        inUse = true;
        if (allowLocal) {
            return DOMAIN_VALIDATOR_WITH_LOCAL;
        }
        return DOMAIN_VALIDATOR;
    }

    /**
     * Private constructor.
     * @param allowLocal whether to allow local domains
     */
    private DomainValidator(boolean allowLocal) {
        this.allowLocal = allowLocal;
    }

    /**
     * Returns true if the specified <code>String</code> parses
     * as a valid domain name with a recognized top-level domain.
     * The parsing is case-insensitive.
     * @param domain the parameter to check for domain name syntax
     * @return true if the parameter is a valid domain name
     */
    @Override
    public boolean isValid(String domain) {
        if (domain == null) {
            return false;
        }
        domain = unicodeToASCII(domain);
        // hosts must be equally reachable via punycode and Unicode
        // Unicode is never shorter than punycode, so check punycode
        // if domain did not convert, then it will be caught by ASCII
        // checks in the regexes below
        if (domain.length() > MAX_DOMAIN_LENGTH) {
            return false;
        }
        String[] groups = domainRegex.match(domain);
        if (groups != null && groups.length > 0) {
            return isValidTld(groups[0]);
        }
        return allowLocal && hostnameRegex.isValid(domain);
    }

    // package protected for unit test access
    // must agree with isValid() above
    boolean isValidDomainSyntax(String domain) {
        if (domain == null) {
            return false;
        }
        domain = unicodeToASCII(domain);
        // hosts must be equally reachable via punycode and Unicode
        // Unicode is never shorter than punycode, so check punycode
        // if domain did not convert, then it will be caught by ASCII
        // checks in the regexes below
        if (domain.length() > MAX_DOMAIN_LENGTH) {
            return false;
        }
        String[] groups = domainRegex.match(domain);
        return (groups != null && groups.length > 0)
                || hostnameRegex.isValid(domain);
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * IANA-defined top-level domain. Leading dots are ignored if present.
     * The search is case-insensitive.
     * @param tld the parameter to check for TLD status, not null
     * @return true if the parameter is a TLD
     */
    public boolean isValidTld(String tld) {
        tld = unicodeToASCII(tld);
        if (allowLocal && isValidLocalTld(tld)) {
            return true;
        }
        return isValidInfrastructureTld(tld)
                || isValidGenericTld(tld)
                || isValidCountryCodeTld(tld);
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * IANA-defined infrastructure top-level domain. Leading dots are
     * ignored if present. The search is case-insensitive.
     * @param iTld the parameter to check for infrastructure TLD status, not null
     * @return true if the parameter is an infrastructure TLD
     */
    public boolean isValidInfrastructureTld(String iTld) {
        final String key = chompLeadingDot(unicodeToASCII(iTld).toLowerCase(Locale.ENGLISH));
        return arrayContains(INFRASTRUCTURE_TLDS, key);
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * IANA-defined generic top-level domain. Leading dots are ignored
     * if present. The search is case-insensitive.
     * @param gTld the parameter to check for generic TLD status, not null
     * @return true if the parameter is a generic TLD
     */
    public boolean isValidGenericTld(String gTld) {
        final String key = chompLeadingDot(unicodeToASCII(gTld).toLowerCase(Locale.ENGLISH));
        return (arrayContains(GENERIC_TLDS, key) || arrayContains(genericTLDsPlus, key))
                && !arrayContains(genericTLDsMinus, key);
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * IANA-defined country code top-level domain. Leading dots are
     * ignored if present. The search is case-insensitive.
     * @param ccTld the parameter to check for country code TLD status, not null
     * @return true if the parameter is a country code TLD
     */
    public boolean isValidCountryCodeTld(String ccTld) {
        final String key = chompLeadingDot(unicodeToASCII(ccTld).toLowerCase(Locale.ENGLISH));
        return (arrayContains(COUNTRY_CODE_TLDS, key) || arrayContains(countryCodeTLDsPlus, key))
                && !arrayContains(countryCodeTLDsMinus, key);
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * widely used "local" domains (localhost or localdomain). Leading dots are
     * ignored if present. The search is case-insensitive.
     * @param lTld the parameter to check for local TLD status, not null
     * @return true if the parameter is an local TLD
     */
    public boolean isValidLocalTld(String lTld) {
        final String key = chompLeadingDot(unicodeToASCII(lTld).toLowerCase(Locale.ENGLISH));
        return arrayContains(LOCAL_TLDS, key);
    }

    private static String chompLeadingDot(String str) {
        if (str.startsWith(".")) {
            return str.substring(1);
        }
        return str;
    }

    // ---------------------------------------------
    // ----- TLDs defined by IANA
    // ----- Authoritative and comprehensive list at:
    // ----- http://data.iana.org/TLD/tlds-alpha-by-domain.txt

    // Note that the above list is in UPPER case.
    // The code currently converts strings to lower case (as per the tables below)

    // IANA also provide an HTML list at http://www.iana.org/domains/root/db
    // Note that this contains several country code entries which are NOT in
    // the text file. These all have the "Not assigned" in the "Sponsoring Organisation" column
    // For example (as of 2015-01-02):
    // .bl  country-code    Not assigned
    // .um  country-code    Not assigned

    // WARNING: this array MUST be sorted, otherwise it cannot be searched reliably using binary search
    private static final String[] INFRASTRUCTURE_TLDS = new String[] {
        "arpa",               // internet infrastructure
    };

    // WARNING: this array MUST be sorted, otherwise it cannot be searched reliably using binary search
    private static final String[] GENERIC_TLDS = new String[] {
         // Taken from Version 2016022601, Last Updated Sat Feb 27 07:07:01 2016 UTC
        "aaa", // aaa American Automobile Association, Inc.
        "aarp", // aarp AARP
        "abb", // abb ABB Ltd
        "abbott", // abbott Abbott Laboratories, Inc.
        "abogado", // abogado Top Level Domain Holdings Limited
        "academy", // academy Half Oaks, LLC
        "accenture", // accenture Accenture plc
        "accountant", // accountant dot Accountant Limited
        "accountants", // accountants Knob Town, LLC
        "aco", // aco ACO Severin Ahlmann GmbH &amp; Co. KG
        "active", // active The Active Network, Inc
        "actor", // actor United TLD Holdco Ltd.
        "adac", // adac Allgemeiner Deutscher Automobil-Club e.V. (ADAC)
        "ads", // ads Charleston Road Registry Inc.
        "adult", // adult ICM Registry AD LLC
        "aeg", // aeg Aktiebolaget Electrolux
        "aero", // aero Societe Internationale de Telecommunications Aeronautique (SITA INC USA)
        "afl", // afl Australian Football League
        "agency", // agency Steel Falls, LLC
        "aig", // aig American International Group, Inc.
        "airforce", // airforce United TLD Holdco Ltd.
        "airtel", // airtel Bharti Airtel Limited
        "alibaba", // alibaba Alibaba Group Holding Limited
        "alipay", // alipay Alibaba Group Holding Limited
        "allfinanz", // allfinanz Allfinanz Deutsche Vermögensberatung Aktiengesellschaft
        "alsace", // alsace REGION D ALSACE
        "amica", // amica Amica Mutual Insurance Company
        "amsterdam", // amsterdam Gemeente Amsterdam
        "analytics", // analytics Campus IP LLC
        "android", // android Charleston Road Registry Inc.
        "apartments", // apartments June Maple, LLC
        "app", // app Charleston Road Registry Inc.
        "apple", // apple Apple Inc.
        "aquarelle", // aquarelle Aquarelle.com
        "aramco", // aramco Aramco Services Company
        "archi", // archi STARTING DOT LIMITED
        "army", // army United TLD Holdco Ltd.
        "arte", // arte Association Relative à la Télévision Européenne G.E.I.E.
        "asia", // asia DotAsia Organisation Ltd.
        "associates", // associates Baxter Hill, LLC
        "attorney", // attorney United TLD Holdco, Ltd
        "auction", // auction United TLD HoldCo, Ltd.
        "audi", // audi AUDI Aktiengesellschaft
        "audio", // audio Uniregistry, Corp.
        "author", // author Amazon Registry Services, Inc.
        "auto", // auto Uniregistry, Corp.
        "autos", // autos DERAutos, LLC
        "axa", // axa AXA SA
        "azure", // azure Microsoft Corporation
        "baidu", // baidu Baidu, Inc.
        "band", // band United TLD Holdco, Ltd
        "bank", // bank fTLD Registry Services, LLC
        "bar", // bar Punto 2012 Sociedad Anonima Promotora de Inversion de Capital Variable
        "barcelona", // barcelona Municipi de Barcelona
        "barclaycard", // barclaycard Barclays Bank PLC
        "barclays", // barclays Barclays Bank PLC
        "bargains", // bargains Half Hallow, LLC
        "bauhaus", // bauhaus Werkhaus GmbH
        "bayern", // bayern Bayern Connect GmbH
        "bbc", // bbc British Broadcasting Corporation
        "bbva", // bbva BANCO BILBAO VIZCAYA ARGENTARIA, S.A.
        "bcn", // bcn Municipi de Barcelona
        "beats", // beats Beats Electronics, LLC
        "beer", // beer Top Level Domain Holdings Limited
        "bentley", // bentley Bentley Motors Limited
        "berlin", // berlin dotBERLIN GmbH &amp; Co. KG
        "best", // best BestTLD Pty Ltd
        "bet", // bet Afilias plc
        "bharti", // bharti Bharti Enterprises (Holding) Private Limited
        "bible", // bible American Bible Society
        "bid", // bid dot Bid Limited
        "bike", // bike Grand Hollow, LLC
        "bing", // bing Microsoft Corporation
        "bingo", // bingo Sand Cedar, LLC
        "bio", // bio STARTING DOT LIMITED
        "biz", // biz Neustar, Inc.
        "black", // black Afilias Limited
        "blackfriday", // blackfriday Uniregistry, Corp.
        "bloomberg", // bloomberg Bloomberg IP Holdings LLC
        "blue", // blue Afilias Limited
        "bms", // bms Bristol-Myers Squibb Company
        "bmw", // bmw Bayerische Motoren Werke Aktiengesellschaft
        "bnl", // bnl Banca Nazionale del Lavoro
        "bnpparibas", // bnpparibas BNP Paribas
        "boats", // boats DERBoats, LLC
        "boehringer", // boehringer Boehringer Ingelheim International GmbH
        "bom", // bom Núcleo de Informação e Coordenação do Ponto BR - NIC.br
        "bond", // bond Bond University Limited
        "boo", // boo Charleston Road Registry Inc.
        "book", // book Amazon Registry Services, Inc.
        "boots", // boots THE BOOTS COMPANY PLC
        "bosch", // bosch Robert Bosch GMBH
        "bostik", // bostik Bostik SA
        "bot", // bot Amazon Registry Services, Inc.
        "boutique", // boutique Over Galley, LLC
        "bradesco", // bradesco Banco Bradesco S.A.
        "bridgestone", // bridgestone Bridgestone Corporation
        "broadway", // broadway Celebrate Broadway, Inc.
        "broker", // broker DOTBROKER REGISTRY LTD
        "brother", // brother Brother Industries, Ltd.
        "brussels", // brussels DNS.be vzw
        "budapest", // budapest Top Level Domain Holdings Limited
        "bugatti", // bugatti Bugatti International SA
        "build", // build Plan Bee LLC
        "builders", // builders Atomic Madison, LLC
        "business", // business Spring Cross, LLC
        "buy", // buy Amazon Registry Services, INC
        "buzz", // buzz DOTSTRATEGY CO.
        "bzh", // bzh Association www.bzh
        "cab", // cab Half Sunset, LLC
        "cafe", // cafe Pioneer Canyon, LLC
        "cal", // cal Charleston Road Registry Inc.
        "call", // call Amazon Registry Services, Inc.
        "camera", // camera Atomic Maple, LLC
        "camp", // camp Delta Dynamite, LLC
        "cancerresearch", // cancerresearch Australian Cancer Research Foundation
        "canon", // canon Canon Inc.
        "capetown", // capetown ZA Central Registry NPC trading as ZA Central Registry
        "capital", // capital Delta Mill, LLC
        "car", // car Cars Registry Limited
        "caravan", // caravan Caravan International, Inc.
        "cards", // cards Foggy Hollow, LLC
        "care", // care Goose Cross, LLC
        "career", // career dotCareer LLC
        "careers", // careers Wild Corner, LLC
        "cars", // cars Uniregistry, Corp.
        "cartier", // cartier Richemont DNS Inc.
        "casa", // casa Top Level Domain Holdings Limited
        "cash", // cash Delta Lake, LLC
        "casino", // casino Binky Sky, LLC
        "cat", // cat Fundacio puntCAT
        "catering", // catering New Falls. LLC
        "cba", // cba COMMONWEALTH BANK OF AUSTRALIA
        "cbn", // cbn The Christian Broadcasting Network, Inc.
        "ceb", // ceb The Corporate Executive Board Company
        "center", // center Tin Mill, LLC
        "ceo", // ceo CEOTLD Pty Ltd
        "cern", // cern European Organization for Nuclear Research (&quot;CERN&quot;)
        "cfa", // cfa CFA Institute
        "cfd", // cfd DOTCFD REGISTRY LTD
        "chanel", // chanel Chanel International B.V.
        "channel", // channel Charleston Road Registry Inc.
        "chat", // chat Sand Fields, LLC
        "cheap", // cheap Sand Cover, LLC
        "chloe", // chloe Richemont DNS Inc.
        "christmas", // christmas Uniregistry, Corp.
        "chrome", // chrome Charleston Road Registry Inc.
        "church", // church Holly Fileds, LLC
        "cipriani", // cipriani Hotel Cipriani Srl
        "circle", // circle Amazon Registry Services, Inc.
        "cisco", // cisco Cisco Technology, Inc.
        "citic", // citic CITIC Group Corporation
        "city", // city Snow Sky, LLC
        "cityeats", // cityeats Lifestyle Domain Holdings, Inc.
        "claims", // claims Black Corner, LLC
        "cleaning", // cleaning Fox Shadow, LLC
        "click", // click Uniregistry, Corp.
        "clinic", // clinic Goose Park, LLC
        "clinique", // clinique The Estée Lauder Companies Inc.
        "clothing", // clothing Steel Lake, LLC
        "cloud", // cloud ARUBA S.p.A.
        "club", // club .CLUB DOMAINS, LLC
        "clubmed", // clubmed Club Méditerranée S.A.
        "coach", // coach Koko Island, LLC
        "codes", // codes Puff Willow, LLC
        "coffee", // coffee Trixy Cover, LLC
        "college", // college XYZ.COM LLC
        "cologne", // cologne NetCologne Gesellschaft für Telekommunikation mbH
        "com", // com VeriSign Global Registry Services
        "commbank", // commbank COMMONWEALTH BANK OF AUSTRALIA
        "community", // community Fox Orchard, LLC
        "company", // company Silver Avenue, LLC
        "compare", // compare iSelect Ltd
        "computer", // computer Pine Mill, LLC
        "comsec", // comsec VeriSign, Inc.
        "condos", // condos Pine House, LLC
        "construction", // construction Fox Dynamite, LLC
        "consulting", // consulting United TLD Holdco, LTD.
        "contact", // contact Top Level Spectrum, Inc.
        "contractors", // contractors Magic Woods, LLC
        "cooking", // cooking Top Level Domain Holdings Limited
        "cool", // cool Koko Lake, LLC
        "coop", // coop DotCooperation LLC
        "corsica", // corsica Collectivité Territoriale de Corse
        "country", // country Top Level Domain Holdings Limited
        "coupon", // coupon Amazon Registry Services, Inc.
        "coupons", // coupons Black Island, LLC
        "courses", // courses OPEN UNIVERSITIES AUSTRALIA PTY LTD
        "credit", // credit Snow Shadow, LLC
        "creditcard", // creditcard Binky Frostbite, LLC
        "creditunion", // creditunion CUNA Performance Resources, LLC
        "cricket", // cricket dot Cricket Limited
        "crown", // crown Crown Equipment Corporation
        "crs", // crs Federated Co-operatives Limited
        "cruises", // cruises Spring Way, LLC
        "csc", // csc Alliance-One Services, Inc.
        "cuisinella", // cuisinella SALM S.A.S.
        "cymru", // cymru Nominet UK
        "cyou", // cyou Beijing Gamease Age Digital Technology Co., Ltd.
        "dabur", // dabur Dabur India Limited
        "dad", // dad Charleston Road Registry Inc.
        "dance", // dance United TLD Holdco Ltd.
        "date", // date dot Date Limited
        "dating", // dating Pine Fest, LLC
        "datsun", // datsun NISSAN MOTOR CO., LTD.
        "day", // day Charleston Road Registry Inc.
        "dclk", // dclk Charleston Road Registry Inc.
        "dealer", // dealer Dealer Dot Com, Inc.
        "deals", // deals Sand Sunset, LLC
        "degree", // degree United TLD Holdco, Ltd
        "delivery", // delivery Steel Station, LLC
        "dell", // dell Dell Inc.
        "deloitte", // deloitte Deloitte Touche Tohmatsu
        "delta", // delta Delta Air Lines, Inc.
        "democrat", // democrat United TLD Holdco Ltd.
        "dental", // dental Tin Birch, LLC
        "dentist", // dentist United TLD Holdco, Ltd
        "desi", // desi Desi Networks LLC
        "design", // design Top Level Design, LLC
        "dev", // dev Charleston Road Registry Inc.
        "diamonds", // diamonds John Edge, LLC
        "diet", // diet Uniregistry, Corp.
        "digital", // digital Dash Park, LLC
        "direct", // direct Half Trail, LLC
        "directory", // directory Extra Madison, LLC
        "discount", // discount Holly Hill, LLC
        "dnp", // dnp Dai Nippon Printing Co., Ltd.
        "docs", // docs Charleston Road Registry Inc.
        "dog", // dog Koko Mill, LLC
        "doha", // doha Communications Regulatory Authority (CRA)
        "domains", // domains Sugar Cross, LLC
        "download", // download dot Support Limited
        "drive", // drive Charleston Road Registry Inc.
        "dubai", // dubai Dubai Smart Government Department
        "durban", // durban ZA Central Registry NPC trading as ZA Central Registry
        "dvag", // dvag Deutsche Vermögensberatung Aktiengesellschaft DVAG
        "earth", // earth Interlink Co., Ltd.
        "eat", // eat Charleston Road Registry Inc.
        "edeka", // edeka EDEKA Verband kaufmännischer Genossenschaften e.V.
        "edu", // edu EDUCAUSE
        "education", // education Brice Way, LLC
        "email", // email Spring Madison, LLC
        "emerck", // emerck Merck KGaA
        "energy", // energy Binky Birch, LLC
        "engineer", // engineer United TLD Holdco Ltd.
        "engineering", // engineering Romeo Canyon
        "enterprises", // enterprises Snow Oaks, LLC
        "epson", // epson Seiko Epson Corporation
        "equipment", // equipment Corn Station, LLC
        "erni", // erni ERNI Group Holding AG
        "esq", // esq Charleston Road Registry Inc.
        "estate", // estate Trixy Park, LLC
        "eurovision", // eurovision European Broadcasting Union (EBU)
        "eus", // eus Puntueus Fundazioa
        "events", // events Pioneer Maple, LLC
        "everbank", // everbank EverBank
        "exchange", // exchange Spring Falls, LLC
        "expert", // expert Magic Pass, LLC
        "exposed", // exposed Victor Beach, LLC
        "express", // express Sea Sunset, LLC
        "fage", // fage Fage International S.A.
        "fail", // fail Atomic Pipe, LLC
        "fairwinds", // fairwinds FairWinds Partners, LLC
        "faith", // faith dot Faith Limited
        "family", // family United TLD Holdco Ltd.
        "fan", // fan Asiamix Digital Ltd
        "fans", // fans Asiamix Digital Limited
        "farm", // farm Just Maple, LLC
        "fashion", // fashion Top Level Domain Holdings Limited
        "fast", // fast Amazon Registry Services, Inc.
        "feedback", // feedback Top Level Spectrum, Inc.
        "ferrero", // ferrero Ferrero Trading Lux S.A.
        "film", // film Motion Picture Domain Registry Pty Ltd
        "final", // final Núcleo de Informação e Coordenação do Ponto BR - NIC.br
        "finance", // finance Cotton Cypress, LLC
        "financial", // financial Just Cover, LLC
        "firestone", // firestone Bridgestone Corporation
        "firmdale", // firmdale Firmdale Holdings Limited
        "fish", // fish Fox Woods, LLC
        "fishing", // fishing Top Level Domain Holdings Limited
        "fit", // fit Minds + Machines Group Limited
        "fitness", // fitness Brice Orchard, LLC
        "flickr", // flickr Yahoo! Domain Services Inc.
        "flights", // flights Fox Station, LLC
        "florist", // florist Half Cypress, LLC
        "flowers", // flowers Uniregistry, Corp.
        "flsmidth", // flsmidth FLSmidth A/S
        "fly", // fly Charleston Road Registry Inc.
        "foo", // foo Charleston Road Registry Inc.
        "football", // football Foggy Farms, LLC
        "ford", // ford Ford Motor Company
        "forex", // forex DOTFOREX REGISTRY LTD
        "forsale", // forsale United TLD Holdco, LLC
        "forum", // forum Fegistry, LLC
        "foundation", // foundation John Dale, LLC
        "fox", // fox FOX Registry, LLC
        "fresenius", // fresenius Fresenius Immobilien-Verwaltungs-GmbH
        "frl", // frl FRLregistry B.V.
        "frogans", // frogans OP3FT
        "frontier", // frontier Frontier Communications Corporation
        "fund", // fund John Castle, LLC
        "furniture", // furniture Lone Fields, LLC
        "futbol", // futbol United TLD Holdco, Ltd.
        "fyi", // fyi Silver Tigers, LLC
        "gal", // gal Asociación puntoGAL
        "gallery", // gallery Sugar House, LLC
        "gallup", // gallup Gallup, Inc.
        "game", // game Uniregistry, Corp.
        "garden", // garden Top Level Domain Holdings Limited
        "gbiz", // gbiz Charleston Road Registry Inc.
        "gdn", // gdn Joint Stock Company "Navigation-information systems"
        "gea", // gea GEA Group Aktiengesellschaft
        "gent", // gent COMBELL GROUP NV/SA
        "genting", // genting Resorts World Inc. Pte. Ltd.
        "ggee", // ggee GMO Internet, Inc.
        "gift", // gift Uniregistry, Corp.
        "gifts", // gifts Goose Sky, LLC
        "gives", // gives United TLD Holdco Ltd.
        "giving", // giving Giving Limited
        "glass", // glass Black Cover, LLC
        "gle", // gle Charleston Road Registry Inc.
        "global", // global Dot Global Domain Registry Limited
        "globo", // globo Globo Comunicação e Participações S.A
        "gmail", // gmail Charleston Road Registry Inc.
        "gmo", // gmo GMO Internet, Inc.
        "gmx", // gmx 1&amp;1 Mail &amp; Media GmbH
        "gold", // gold June Edge, LLC
        "goldpoint", // goldpoint YODOBASHI CAMERA CO.,LTD.
        "golf", // golf Lone Falls, LLC
        "goo", // goo NTT Resonant Inc.
        "goog", // goog Charleston Road Registry Inc.
        "google", // google Charleston Road Registry Inc.
        "gop", // gop Republican State Leadership Committee, Inc.
        "got", // got Amazon Registry Services, Inc.
        "gov", // gov General Services Administration Attn: QTDC, 2E08 (.gov Domain Registration)
        "grainger", // grainger Grainger Registry Services, LLC
        "graphics", // graphics Over Madison, LLC
        "gratis", // gratis Pioneer Tigers, LLC
        "green", // green Afilias Limited
        "gripe", // gripe Corn Sunset, LLC
        "group", // group Romeo Town, LLC
        "gucci", // gucci Guccio Gucci S.p.a.
        "guge", // guge Charleston Road Registry Inc.
        "guide", // guide Snow Moon, LLC
        "guitars", // guitars Uniregistry, Corp.
        "guru", // guru Pioneer Cypress, LLC
        "hamburg", // hamburg Hamburg Top-Level-Domain GmbH
        "hangout", // hangout Charleston Road Registry Inc.
        "haus", // haus United TLD Holdco, LTD.
        "hdfcbank", // hdfcbank HDFC Bank Limited
        "health", // health DotHealth, LLC
        "healthcare", // healthcare Silver Glen, LLC
        "help", // help Uniregistry, Corp.
        "helsinki", // helsinki City of Helsinki
        "here", // here Charleston Road Registry Inc.
        "hermes", // hermes Hermes International
        "hiphop", // hiphop Uniregistry, Corp.
        "hitachi", // hitachi Hitachi, Ltd.
        "hiv", // hiv dotHIV gemeinnuetziger e.V.
        "hockey", // hockey Half Willow, LLC
        "holdings", // holdings John Madison, LLC
        "holiday", // holiday Goose Woods, LLC
        "homedepot", // homedepot Homer TLC, Inc.
        "homes", // homes DERHomes, LLC
        "honda", // honda Honda Motor Co., Ltd.
        "horse", // horse Top Level Domain Holdings Limited
        "host", // host DotHost Inc.
        "hosting", // hosting Uniregistry, Corp.
        "hoteles", // hoteles Travel Reservations SRL
        "hotmail", // hotmail Microsoft Corporation
        "house", // house Sugar Park, LLC
        "how", // how Charleston Road Registry Inc.
        "hsbc", // hsbc HSBC Holdings PLC
        "hyundai", // hyundai Hyundai Motor Company
        "ibm", // ibm International Business Machines Corporation
        "icbc", // icbc Industrial and Commercial Bank of China Limited
        "ice", // ice IntercontinentalExchange, Inc.
        "icu", // icu One.com A/S
        "ifm", // ifm ifm electronic gmbh
        "iinet", // iinet Connect West Pty. Ltd.
        "immo", // immo Auburn Bloom, LLC
        "immobilien", // immobilien United TLD Holdco Ltd.
        "industries", // industries Outer House, LLC
        "infiniti", // infiniti NISSAN MOTOR CO., LTD.
        "info", // info Afilias Limited
        "ing", // ing Charleston Road Registry Inc.
        "ink", // ink Top Level Design, LLC
        "institute", // institute Outer Maple, LLC
        "insurance", // insurance fTLD Registry Services LLC
        "insure", // insure Pioneer Willow, LLC
        "int", // int Internet Assigned Numbers Authority
        "international", // international Wild Way, LLC
        "investments", // investments Holly Glen, LLC
        "ipiranga", // ipiranga Ipiranga Produtos de Petroleo S.A.
        "irish", // irish Dot-Irish LLC
        "iselect", // iselect iSelect Ltd
        "ist", // ist Istanbul Metropolitan Municipality
        "istanbul", // istanbul Istanbul Metropolitan Municipality / Medya A.S.
        "itau", // itau Itau Unibanco Holding S.A.
        "iwc", // iwc Richemont DNS Inc.
        "jaguar", // jaguar Jaguar Land Rover Ltd
        "java", // java Oracle Corporation
        "jcb", // jcb JCB Co., Ltd.
        "jetzt", // jetzt New TLD Company AB
        "jewelry", // jewelry Wild Bloom, LLC
        "jlc", // jlc Richemont DNS Inc.
        "jll", // jll Jones Lang LaSalle Incorporated
        "jmp", // jmp Matrix IP LLC
        "jobs", // jobs Employ Media LLC
        "joburg", // joburg ZA Central Registry NPC trading as ZA Central Registry
        "jot", // jot Amazon Registry Services, Inc.
        "joy", // joy Amazon Registry Services, Inc.
        "jprs", // jprs Japan Registry Services Co., Ltd.
        "juegos", // juegos Uniregistry, Corp.
        "kaufen", // kaufen United TLD Holdco Ltd.
        "kddi", // kddi KDDI CORPORATION
        "kfh", // kfh Kuwait Finance House
        "kia", // kia KIA MOTORS CORPORATION
        "kim", // kim Afilias Limited
        "kinder", // kinder Ferrero Trading Lux S.A.
        "kitchen", // kitchen Just Goodbye, LLC
        "kiwi", // kiwi DOT KIWI LIMITED
        "koeln", // koeln NetCologne Gesellschaft für Telekommunikation mbH
        "komatsu", // komatsu Komatsu Ltd.
        "kpn", // kpn Koninklijke KPN N.V.
        "krd", // krd KRG Department of Information Technology
        "kred", // kred KredTLD Pty Ltd
        "kyoto", // kyoto Academic Institution: Kyoto Jyoho Gakuen
        "lacaixa", // lacaixa CAIXA D&#39;ESTALVIS I PENSIONS DE BARCELONA
        "lamborghini", // lamborghini Automobili Lamborghini S.p.A.
        "lamer", // lamer The Estée Lauder Companies Inc.
        "lancaster", // lancaster LANCASTER
        "land", // land Pine Moon, LLC
        "landrover", // landrover Jaguar Land Rover Ltd
        "lanxess", // lanxess LANXESS Corporation
        "lasalle", // lasalle Jones Lang LaSalle Incorporated
        "lat", // lat ECOM-LAC Federación de Latinoamérica y el Caribe para Internet y el Comercio Electrónico
        "latrobe", // latrobe La Trobe University
        "law", // law Minds + Machines Group Limited
        "lawyer", // lawyer United TLD Holdco, Ltd
        "lds", // lds IRI Domain Management, LLC
        "lease", // lease Victor Trail, LLC
        "leclerc", // leclerc A.C.D. LEC Association des Centres Distributeurs Edouard Leclerc
        "legal", // legal Blue Falls, LLC
        "lexus", // lexus TOYOTA MOTOR CORPORATION
        "lgbt", // lgbt Afilias Limited
        "liaison", // liaison Liaison Technologies, Incorporated
        "lidl", // lidl Schwarz Domains und Services GmbH &amp; Co. KG
        "life", // life Trixy Oaks, LLC
        "lifeinsurance", // lifeinsurance American Council of Life Insurers
        "lifestyle", // lifestyle Lifestyle Domain Holdings, Inc.
        "lighting", // lighting John McCook, LLC
        "like", // like Amazon Registry Services, Inc.
        "limited", // limited Big Fest, LLC
        "limo", // limo Hidden Frostbite, LLC
        "lincoln", // lincoln Ford Motor Company
        "linde", // linde Linde Aktiengesellschaft
        "link", // link Uniregistry, Corp.
        "live", // live United TLD Holdco Ltd.
        "living", // living Lifestyle Domain Holdings, Inc.
        "lixil", // lixil LIXIL Group Corporation
        "loan", // loan dot Loan Limited
        "loans", // loans June Woods, LLC
        "lol", // lol Uniregistry, Corp.
        "london", // london Dot London Domains Limited
        "lotte", // lotte Lotte Holdings Co., Ltd.
        "lotto", // lotto Afilias Limited
        "love", // love Merchant Law Group LLP
        "ltd", // ltd Over Corner, LLC
        "ltda", // ltda InterNetX Corp.
        "lupin", // lupin LUPIN LIMITED
        "luxe", // luxe Top Level Domain Holdings Limited
        "luxury", // luxury Luxury Partners LLC
        "madrid", // madrid Comunidad de Madrid
        "maif", // maif Mutuelle Assurance Instituteur France (MAIF)
        "maison", // maison Victor Frostbite, LLC
        "makeup", // makeup L&#39;Oréal
        "man", // man MAN SE
        "management", // management John Goodbye, LLC
        "mango", // mango PUNTO FA S.L.
        "market", // market Unitied TLD Holdco, Ltd
        "marketing", // marketing Fern Pass, LLC
        "markets", // markets DOTMARKETS REGISTRY LTD
        "marriott", // marriott Marriott Worldwide Corporation
        "mba", // mba Lone Hollow, LLC
        "med", // med Medistry LLC
        "media", // media Grand Glen, LLC
        "meet", // meet Afilias Limited
        "melbourne", // melbourne The Crown in right of the State of Victoria
        "meme", // meme Charleston Road Registry Inc.
        "memorial", // memorial Dog Beach, LLC
        "men", // men Exclusive Registry Limited
        "menu", // menu Wedding TLD2, LLC
        "meo", // meo PT Comunicacoes S.A.
        "miami", // miami Top Level Domain Holdings Limited
        "microsoft", // microsoft Microsoft Corporation
        "mil", // mil DoD Network Information Center
        "mini", // mini Bayerische Motoren Werke Aktiengesellschaft
        "mma", // mma MMA IARD
        "mobi", // mobi Afilias Technologies Limited dba dotMobi
        "mobily", // mobily GreenTech Consultancy Company W.L.L.
        "moda", // moda United TLD Holdco Ltd.
        "moe", // moe Interlink Co., Ltd.
        "moi", // moi Amazon Registry Services, Inc.
        "mom", // mom Uniregistry, Corp.
        "monash", // monash Monash University
        "money", // money Outer McCook, LLC
        "montblanc", // montblanc Richemont DNS Inc.
        "mormon", // mormon IRI Domain Management, LLC (&quot;Applicant&quot;)
        "mortgage", // mortgage United TLD Holdco, Ltd
        "moscow", // moscow Foundation for Assistance for Internet Technologies and Infrastructure Development (FAITID)
        "motorcycles", // motorcycles DERMotorcycles, LLC
        "mov", // mov Charleston Road Registry Inc.
        "movie", // movie New Frostbite, LLC
        "movistar", // movistar Telefónica S.A.
        "mtn", // mtn MTN Dubai Limited
        "mtpc", // mtpc Mitsubishi Tanabe Pharma Corporation
        "mtr", // mtr MTR Corporation Limited
        "museum", // museum Museum Domain Management Association
        "mutuelle", // mutuelle Fédération Nationale de la Mutualité Française
        "nadex", // nadex Nadex Domains, Inc
        "nagoya", // nagoya GMO Registry, Inc.
        "name", // name VeriSign Information Services, Inc.
        "natura", // natura NATURA COSMÉTICOS S.A.
        "navy", // navy United TLD Holdco Ltd.
        "nec", // nec NEC Corporation
        "net", // net VeriSign Global Registry Services
        "netbank", // netbank COMMONWEALTH BANK OF AUSTRALIA
        "network", // network Trixy Manor, LLC
        "neustar", // neustar NeuStar, Inc.
        "new", // new Charleston Road Registry Inc.
        "news", // news United TLD Holdco Ltd.
        "nexus", // nexus Charleston Road Registry Inc.
        "ngo", // ngo Public Interest Registry
        "nhk", // nhk Japan Broadcasting Corporation (NHK)
        "nico", // nico DWANGO Co., Ltd.
        "nikon", // nikon NIKON CORPORATION
        "ninja", // ninja United TLD Holdco Ltd.
        "nissan", // nissan NISSAN MOTOR CO., LTD.
        "nokia", // nokia Nokia Corporation
        "norton", // norton Symantec Corporation
        "nowruz", // nowruz Asia Green IT System Bilgisayar San. ve Tic. Ltd. Sti.
        "nra", // nra NRA Holdings Company, INC.
        "nrw", // nrw Minds + Machines GmbH
        "ntt", // ntt NIPPON TELEGRAPH AND TELEPHONE CORPORATION
        "nyc", // nyc The City of New York by and through the New York City Department of Information Technology &amp; Telecommunications
        "obi", // obi OBI Group Holding SE &amp; Co. KGaA
        "office", // office Microsoft Corporation
        "okinawa", // okinawa BusinessRalliart inc.
        "omega", // omega The Swatch Group Ltd
        "one", // one One.com A/S
        "ong", // ong Public Interest Registry
        "onl", // onl I-REGISTRY Ltd., Niederlassung Deutschland
        "online", // online DotOnline Inc.
        "ooo", // ooo INFIBEAM INCORPORATION LIMITED
        "oracle", // oracle Oracle Corporation
        "orange", // orange Orange Brand Services Limited
        "org", // org Public Interest Registry (PIR)
        "organic", // organic Afilias Limited
        "origins", // origins The Estée Lauder Companies Inc.
        "osaka", // osaka Interlink Co., Ltd.
        "otsuka", // otsuka Otsuka Holdings Co., Ltd.
        "ovh", // ovh OVH SAS
        "page", // page Charleston Road Registry Inc.
        "pamperedchef", // pamperedchef The Pampered Chef, Ltd.
        "panerai", // panerai Richemont DNS Inc.
        "paris", // paris City of Paris
        "pars", // pars Asia Green IT System Bilgisayar San. ve Tic. Ltd. Sti.
        "partners", // partners Magic Glen, LLC
        "parts", // parts Sea Goodbye, LLC
        "party", // party Blue Sky Registry Limited
        "pet", // pet Afilias plc
        "pharmacy", // pharmacy National Association of Boards of Pharmacy
        "philips", // philips Koninklijke Philips N.V.
        "photo", // photo Uniregistry, Corp.
        "photography", // photography Sugar Glen, LLC
        "photos", // photos Sea Corner, LLC
        "physio", // physio PhysBiz Pty Ltd
        "piaget", // piaget Richemont DNS Inc.
        "pics", // pics Uniregistry, Corp.
        "pictet", // pictet Pictet Europe S.A.
        "pictures", // pictures Foggy Sky, LLC
        "pid", // pid Top Level Spectrum, Inc.
        "pin", // pin Amazon Registry Services, Inc.
        "ping", // ping Ping Registry Provider, Inc.
        "pink", // pink Afilias Limited
        "pizza", // pizza Foggy Moon, LLC
        "place", // place Snow Galley, LLC
        "play", // play Charleston Road Registry Inc.
        "playstation", // playstation Sony Computer Entertainment Inc.
        "plumbing", // plumbing Spring Tigers, LLC
        "plus", // plus Sugar Mill, LLC
        "pohl", // pohl Deutsche Vermögensberatung Aktiengesellschaft DVAG
        "poker", // poker Afilias Domains No. 5 Limited
        "porn", // porn ICM Registry PN LLC
        "post", // post Universal Postal Union
        "praxi", // praxi Praxi S.p.A.
        "press", // press DotPress Inc.
        "pro", // pro Registry Services Corporation dba RegistryPro
        "prod", // prod Charleston Road Registry Inc.
        "productions", // productions Magic Birch, LLC
        "prof", // prof Charleston Road Registry Inc.
        "promo", // promo Afilias plc
        "properties", // properties Big Pass, LLC
        "property", // property Uniregistry, Corp.
        "protection", // protection XYZ.COM LLC
        "pub", // pub United TLD Holdco Ltd.
        "pwc", // pwc PricewaterhouseCoopers LLP
        "qpon", // qpon dotCOOL, Inc.
        "quebec", // quebec PointQuébec Inc
        "quest", // quest Quest ION Limited
        "racing", // racing Premier Registry Limited
        "read", // read Amazon Registry Services, Inc.
        "realtor", // realtor Real Estate Domains LLC
        "realty", // realty Fegistry, LLC
        "recipes", // recipes Grand Island, LLC
        "red", // red Afilias Limited
        "redstone", // redstone Redstone Haute Couture Co., Ltd.
        "redumbrella", // redumbrella Travelers TLD, LLC
        "rehab", // rehab United TLD Holdco Ltd.
        "reise", // reise Foggy Way, LLC
        "reisen", // reisen New Cypress, LLC
        "reit", // reit National Association of Real Estate Investment Trusts, Inc.
        "ren", // ren Beijing Qianxiang Wangjing Technology Development Co., Ltd.
        "rent", // rent XYZ.COM LLC
        "rentals", // rentals Big Hollow,LLC
        "repair", // repair Lone Sunset, LLC
        "report", // report Binky Glen, LLC
        "republican", // republican United TLD Holdco Ltd.
        "rest", // rest Punto 2012 Sociedad Anonima Promotora de Inversion de Capital Variable
        "restaurant", // restaurant Snow Avenue, LLC
        "review", // review dot Review Limited
        "reviews", // reviews United TLD Holdco, Ltd.
        "rexroth", // rexroth Robert Bosch GMBH
        "rich", // rich I-REGISTRY Ltd., Niederlassung Deutschland
        "ricoh", // ricoh Ricoh Company, Ltd.
        "rio", // rio Empresa Municipal de Informática SA - IPLANRIO
        "rip", // rip United TLD Holdco Ltd.
        "rocher", // rocher Ferrero Trading Lux S.A.
        "rocks", // rocks United TLD Holdco, LTD.
        "rodeo", // rodeo Top Level Domain Holdings Limited
        "room", // room Amazon Registry Services, Inc.
        "rsvp", // rsvp Charleston Road Registry Inc.
        "ruhr", // ruhr regiodot GmbH &amp; Co. KG
        "run", // run Snow Park, LLC
        "rwe", // rwe RWE AG
        "ryukyu", // ryukyu BusinessRalliart inc.
        "saarland", // saarland dotSaarland GmbH
        "safe", // safe Amazon Registry Services, Inc.
        "safety", // safety Safety Registry Services, LLC.
        "sakura", // sakura SAKURA Internet Inc.
        "sale", // sale United TLD Holdco, Ltd
        "salon", // salon Outer Orchard, LLC
        "samsung", // samsung SAMSUNG SDS CO., LTD
        "sandvik", // sandvik Sandvik AB
        "sandvikcoromant", // sandvikcoromant Sandvik AB
        "sanofi", // sanofi Sanofi
        "sap", // sap SAP AG
        "sapo", // sapo PT Comunicacoes S.A.
        "sarl", // sarl Delta Orchard, LLC
        "sas", // sas Research IP LLC
        "saxo", // saxo Saxo Bank A/S
        "sbs", // sbs SPECIAL BROADCASTING SERVICE CORPORATION
        "sca", // sca SVENSKA CELLULOSA AKTIEBOLAGET SCA (publ)
        "scb", // scb The Siam Commercial Bank Public Company Limited (&quot;SCB&quot;)
        "schaeffler", // schaeffler Schaeffler Technologies AG &amp; Co. KG
        "schmidt", // schmidt SALM S.A.S.
        "scholarships", // scholarships Scholarships.com, LLC
        "school", // school Little Galley, LLC
        "schule", // schule Outer Moon, LLC
        "schwarz", // schwarz Schwarz Domains und Services GmbH &amp; Co. KG
        "science", // science dot Science Limited
        "scor", // scor SCOR SE
        "scot", // scot Dot Scot Registry Limited
        "seat", // seat SEAT, S.A. (Sociedad Unipersonal)
        "security", // security XYZ.COM LLC
        "seek", // seek Seek Limited
        "select", // select iSelect Ltd
        "sener", // sener Sener Ingeniería y Sistemas, S.A.
        "services", // services Fox Castle, LLC
        "seven", // seven Seven West Media Ltd
        "sew", // sew SEW-EURODRIVE GmbH &amp; Co KG
        "sex", // sex ICM Registry SX LLC
        "sexy", // sexy Uniregistry, Corp.
        "sfr", // sfr Societe Francaise du Radiotelephone - SFR
        "sharp", // sharp Sharp Corporation
        "shell", // shell Shell Information Technology International Inc
        "shia", // shia Asia Green IT System Bilgisayar San. ve Tic. Ltd. Sti.
        "shiksha", // shiksha Afilias Limited
        "shoes", // shoes Binky Galley, LLC
        "show", // show Snow Beach, LLC
        "shriram", // shriram Shriram Capital Ltd.
        "singles", // singles Fern Madison, LLC
        "site", // site DotSite Inc.
        "ski", // ski STARTING DOT LIMITED
        "skin", // skin L&#39;Oréal
        "sky", // sky Sky International AG
        "skype", // skype Microsoft Corporation
        "smile", // smile Amazon Registry Services, Inc.
        "sncf", // sncf SNCF (Société Nationale des Chemins de fer Francais)
        "soccer", // soccer Foggy Shadow, LLC
        "social", // social United TLD Holdco Ltd.
        "softbank", // softbank SoftBank Group Corp.
        "software", // software United TLD Holdco, Ltd
        "sohu", // sohu Sohu.com Limited
        "solar", // solar Ruby Town, LLC
        "solutions", // solutions Silver Cover, LLC
        "song", // song Amazon EU S.à r.l.
        "sony", // sony Sony Corporation
        "soy", // soy Charleston Road Registry Inc.
        "space", // space DotSpace Inc.
        "spiegel", // spiegel SPIEGEL-Verlag Rudolf Augstein GmbH &amp; Co. KG
        "spot", // spot Amazon Registry Services, Inc.
        "spreadbetting", // spreadbetting DOTSPREADBETTING REGISTRY LTD
        "srl", // srl InterNetX Corp.
        "stada", // stada STADA Arzneimittel AG
        "star", // star Star India Private Limited
        "starhub", // starhub StarHub Limited
        "statefarm", // statefarm State Farm Mutual Automobile Insurance Company
        "statoil", // statoil Statoil ASA
        "stc", // stc Saudi Telecom Company
        "stcgroup", // stcgroup Saudi Telecom Company
        "stockholm", // stockholm Stockholms kommun
        "storage", // storage Self Storage Company LLC
        "store", // store DotStore Inc.
        "studio", // studio United TLD Holdco Ltd.
        "study", // study OPEN UNIVERSITIES AUSTRALIA PTY LTD
        "style", // style Binky Moon, LLC
        "sucks", // sucks Vox Populi Registry Ltd.
        "supplies", // supplies Atomic Fields, LLC
        "supply", // supply Half Falls, LLC
        "support", // support Grand Orchard, LLC
        "surf", // surf Top Level Domain Holdings Limited
        "surgery", // surgery Tin Avenue, LLC
        "suzuki", // suzuki SUZUKI MOTOR CORPORATION
        "swatch", // swatch The Swatch Group Ltd
        "swiss", // swiss Swiss Confederation
        "sydney", // sydney State of New South Wales, Department of Premier and Cabinet
        "symantec", // symantec Symantec Corporation
        "systems", // systems Dash Cypress, LLC
        "tab", // tab Tabcorp Holdings Limited
        "taipei", // taipei Taipei City Government
        "taobao", // taobao Alibaba Group Holding Limited
        "tatamotors", // tatamotors Tata Motors Ltd
        "tatar", // tatar Limited Liability Company "Coordination Center of Regional Domain of Tatarstan Republic"
        "tattoo", // tattoo Uniregistry, Corp.
        "tax", // tax Storm Orchard, LLC
        "taxi", // taxi Pine Falls, LLC
        "tci", // tci Asia Green IT System Bilgisayar San. ve Tic. Ltd. Sti.
        "team", // team Atomic Lake, LLC
        "tech", // tech Dot Tech LLC
        "technology", // technology Auburn Falls, LLC
        "tel", // tel Telnic Ltd.
        "telecity", // telecity TelecityGroup International Limited
        "telefonica", // telefonica Telefónica S.A.
        "temasek", // temasek Temasek Holdings (Private) Limited
        "tennis", // tennis Cotton Bloom, LLC
        "thd", // thd Homer TLC, Inc.
        "theater", // theater Blue Tigers, LLC
        "theatre", // theatre XYZ.COM LLC
        "tickets", // tickets Accent Media Limited
        "tienda", // tienda Victor Manor, LLC
        "tiffany", // tiffany Tiffany and Company
        "tips", // tips Corn Willow, LLC
        "tires", // tires Dog Edge, LLC
        "tirol", // tirol punkt Tirol GmbH
        "tmall", // tmall Alibaba Group Holding Limited
        "today", // today Pearl Woods, LLC
        "tokyo", // tokyo GMO Registry, Inc.
        "tools", // tools Pioneer North, LLC
        "top", // top Jiangsu Bangning Science &amp; Technology Co.,Ltd.
        "toray", // toray Toray Industries, Inc.
        "toshiba", // toshiba TOSHIBA Corporation
        "tours", // tours Sugar Station, LLC
        "town", // town Koko Moon, LLC
        "toyota", // toyota TOYOTA MOTOR CORPORATION
        "toys", // toys Pioneer Orchard, LLC
        "trade", // trade Elite Registry Limited
        "trading", // trading DOTTRADING REGISTRY LTD
        "training", // training Wild Willow, LLC
        "travel", // travel Tralliance Registry Management Company, LLC.
        "travelers", // travelers Travelers TLD, LLC
        "travelersinsurance", // travelersinsurance Travelers TLD, LLC
        "trust", // trust Artemis Internet Inc
        "trv", // trv Travelers TLD, LLC
        "tube", // tube Latin American Telecom LLC
        "tui", // tui TUI AG
        "tunes", // tunes Amazon Registry Services, Inc.
        "tushu", // tushu Amazon Registry Services, Inc.
        "tvs", // tvs T V SUNDRAM IYENGAR  &amp; SONS PRIVATE LIMITED
        "ubs", // ubs UBS AG
        "unicom", // unicom China United Network Communications Corporation Limited
        "university", // university Little Station, LLC
        "uno", // uno Dot Latin LLC
        "uol", // uol UBN INTERNET LTDA.
        "vacations", // vacations Atomic Tigers, LLC
        "vana", // vana Lifestyle Domain Holdings, Inc.
        "vegas", // vegas Dot Vegas, Inc.
        "ventures", // ventures Binky Lake, LLC
        "verisign", // verisign VeriSign, Inc.
        "versicherung", // versicherung dotversicherung-registry GmbH
        "vet", // vet United TLD Holdco, Ltd
        "viajes", // viajes Black Madison, LLC
        "video", // video United TLD Holdco, Ltd
        "viking", // viking Viking River Cruises (Bermuda) Ltd.
        "villas", // villas New Sky, LLC
        "vin", // vin Holly Shadow, LLC
        "vip", // vip Minds + Machines Group Limited
        "virgin", // virgin Virgin Enterprises Limited
        "vision", // vision Koko Station, LLC
        "vista", // vista Vistaprint Limited
        "vistaprint", // vistaprint Vistaprint Limited
        "viva", // viva Saudi Telecom Company
        "vlaanderen", // vlaanderen DNS.be vzw
        "vodka", // vodka Top Level Domain Holdings Limited
        "volkswagen", // volkswagen Volkswagen Group of America Inc.
        "vote", // vote Monolith Registry LLC
        "voting", // voting Valuetainment Corp.
        "voto", // voto Monolith Registry LLC
        "voyage", // voyage Ruby House, LLC
        "wales", // wales Nominet UK
        "walter", // walter Sandvik AB
        "wang", // wang Zodiac Registry Limited
        "wanggou", // wanggou Amazon Registry Services, Inc.
        "watch", // watch Sand Shadow, LLC
        "watches", // watches Richemont DNS Inc.
        "weather", // weather The Weather Channel, LLC
        "weatherchannel", // weatherchannel The Weather Channel, LLC
        "webcam", // webcam dot Webcam Limited
        "weber", // weber Saint-Gobain Weber SA
        "website", // website DotWebsite Inc.
        "wed", // wed Atgron, Inc.
        "wedding", // wedding Top Level Domain Holdings Limited
        "weir", // weir Weir Group IP Limited
        "whoswho", // whoswho Who&#39;s Who Registry
        "wien", // wien punkt.wien GmbH
        "wiki", // wiki Top Level Design, LLC
        "williamhill", // williamhill William Hill Organization Limited
        "win", // win First Registry Limited
        "windows", // windows Microsoft Corporation
        "wine", // wine June Station, LLC
        "wme", // wme William Morris Endeavor Entertainment, LLC
        "wolterskluwer", // wolterskluwer Wolters Kluwer N.V.
        "work", // work Top Level Domain Holdings Limited
        "works", // works Little Dynamite, LLC
        "world", // world Bitter Fields, LLC
        "wtc", // wtc World Trade Centers Association, Inc.
        "wtf", // wtf Hidden Way, LLC
        "xbox", // xbox Microsoft Corporation
        "xerox", // xerox Xerox DNHC LLC
        "xin", // xin Elegant Leader Limited
        "xn--11b4c3d", // कॉम VeriSign Sarl
        "xn--1ck2e1b", // セール Amazon Registry Services, Inc.
        "xn--1qqw23a", // 佛山 Guangzhou YU Wei Information Technology Co., Ltd.
        "xn--30rr7y", // 慈善 Excellent First Limited
        "xn--3bst00m", // 集团 Eagle Horizon Limited
        "xn--3ds443g", // 在线 TLD REGISTRY LIMITED
        "xn--3pxu8k", // 点看 VeriSign Sarl
        "xn--42c2d9a", // คอม VeriSign Sarl
        "xn--45q11c", // 八卦 Zodiac Scorpio Limited
        "xn--4gbrim", // موقع Suhub Electronic Establishment
        "xn--55qw42g", // 公益 China Organizational Name Administration Center
        "xn--55qx5d", // 公司 Computer Network Information Center of Chinese Academy of Sciences （China Internet Network Information Center）
        "xn--6frz82g", // 移动 Afilias Limited
        "xn--6qq986b3xl", // 我爱你 Tycoon Treasure Limited
        "xn--80adxhks", // москва Foundation for Assistance for Internet Technologies and Infrastructure Development (FAITID)
        "xn--80asehdb", // онлайн CORE Association
        "xn--80aswg", // сайт CORE Association
        "xn--8y0a063a", // 联通 China United Network Communications Corporation Limited
        "xn--9dbq2a", // קום VeriSign Sarl
        "xn--9et52u", // 时尚 RISE VICTORY LIMITED
        "xn--b4w605ferd", // 淡马锡 Temasek Holdings (Private) Limited
        "xn--bck1b9a5dre4c", // ファッション Amazon Registry Services, Inc.
        "xn--c1avg", // орг Public Interest Registry
        "xn--c2br7g", // नेट VeriSign Sarl
        "xn--cck2b3b", // ストア Amazon Registry Services, Inc.
        "xn--cg4bki", // 삼성 SAMSUNG SDS CO., LTD
        "xn--czr694b", // 商标 HU YI GLOBAL INFORMATION RESOURCES(HOLDING) COMPANY.HONGKONG LIMITED
        "xn--czrs0t", // 商店 Wild Island, LLC
        "xn--czru2d", // 商城 Zodiac Aquarius Limited
        "xn--d1acj3b", // дети The Foundation for Network Initiatives “The Smart Internet”
        "xn--eckvdtc9d", // ポイント Amazon Registry Services, Inc.
        "xn--efvy88h", // 新闻 Xinhua News Agency Guangdong Branch 新华通讯社广东分社
        "xn--estv75g", // 工行 Industrial and Commercial Bank of China Limited
        "xn--fhbei", // كوم VeriSign Sarl
        "xn--fiq228c5hs", // 中文网 TLD REGISTRY LIMITED
        "xn--fiq64b", // 中信 CITIC Group Corporation
        "xn--fjq720a", // 娱乐 Will Bloom, LLC
        "xn--flw351e", // 谷歌 Charleston Road Registry Inc.
        "xn--g2xx48c", // 购物 Minds + Machines Group Limited
        "xn--gckr3f0f", // クラウド Amazon Registry Services, Inc.
        "xn--hxt814e", // 网店 Zodiac Libra Limited
        "xn--i1b6b1a6a2e", // संगठन Public Interest Registry
        "xn--imr513n", // 餐厅 HU YI GLOBAL INFORMATION RESOURCES (HOLDING) COMPANY. HONGKONG LIMITED
        "xn--io0a7i", // 网络 Computer Network Information Center of Chinese Academy of Sciences （China Internet Network Information Center）
        "xn--j1aef", // ком VeriSign Sarl
        "xn--jlq61u9w7b", // 诺基亚 Nokia Corporation
        "xn--jvr189m", // 食品 Amazon Registry Services, Inc.
        "xn--kcrx77d1x4a", // 飞利浦 Koninklijke Philips N.V.
        "xn--kpu716f", // 手表 Richemont DNS Inc.
        "xn--kput3i", // 手机 Beijing RITT-Net Technology Development Co., Ltd
        "xn--mgba3a3ejt", // ارامكو Aramco Services Company
        "xn--mgbab2bd", // بازار CORE Association
        "xn--mgbb9fbpob", // موبايلي GreenTech Consultancy Company W.L.L.
        "xn--mgbt3dhd", // همراه Asia Green IT System Bilgisayar San. ve Tic. Ltd. Sti.
        "xn--mk1bu44c", // 닷컴 VeriSign Sarl
        "xn--mxtq1m", // 政府 Net-Chinese Co., Ltd.
        "xn--ngbc5azd", // شبكة International Domain Registry Pty. Ltd.
        "xn--ngbe9e0a", // بيتك Kuwait Finance House
        "xn--nqv7f", // 机构 Public Interest Registry
        "xn--nqv7fs00ema", // 组织机构 Public Interest Registry
        "xn--nyqy26a", // 健康 Stable Tone Limited
        "xn--p1acf", // рус Rusnames Limited
        "xn--pbt977c", // 珠宝 Richemont DNS Inc.
        "xn--pssy2u", // 大拿 VeriSign Sarl
        "xn--q9jyb4c", // みんな Charleston Road Registry Inc.
        "xn--qcka1pmc", // グーグル Charleston Road Registry Inc.
        "xn--rhqv96g", // 世界 Stable Tone Limited
        "xn--rovu88b", // 書籍 Amazon EU S.à r.l.
        "xn--ses554g", // 网址 KNET Co., Ltd
        "xn--t60b56a", // 닷넷 VeriSign Sarl
        "xn--tckwe", // コム VeriSign Sarl
        "xn--unup4y", // 游戏 Spring Fields, LLC
        "xn--vermgensberater-ctb", // VERMöGENSBERATER Deutsche Vermögensberatung Aktiengesellschaft DVAG
        "xn--vermgensberatung-pwb", // VERMöGENSBERATUNG Deutsche Vermögensberatung Aktiengesellschaft DVAG
        "xn--vhquv", // 企业 Dash McCook, LLC
        "xn--vuq861b", // 信息 Beijing Tele-info Network Technology Co., Ltd.
        "xn--xhq521b", // 广东 Guangzhou YU Wei Information Technology Co., Ltd.
        "xn--zfr164b", // 政务 China Organizational Name Administration Center
        "xperia", // xperia Sony Mobile Communications AB
        "xxx", // xxx ICM Registry LLC
        "xyz", // xyz XYZ.COM LLC
        "yachts", // yachts DERYachts, LLC
        "yahoo", // yahoo Yahoo! Domain Services Inc.
        "yamaxun", // yamaxun Amazon Registry Services, Inc.
        "yandex", // yandex YANDEX, LLC
        "yodobashi", // yodobashi YODOBASHI CAMERA CO.,LTD.
        "yoga", // yoga Top Level Domain Holdings Limited
        "yokohama", // yokohama GMO Registry, Inc.
        "youtube", // youtube Charleston Road Registry Inc.
        "zara", // zara Industria de Diseño Textil, S.A. (INDITEX, S.A.)
        "zero", // zero Amazon Registry Services, Inc.
        "zip", // zip Charleston Road Registry Inc.
        "zone", // zone Outer Falls, LLC
        "zuerich", // zuerich Kanton Zürich (Canton of Zurich)
    };

    // WARNING: this array MUST be sorted, otherwise it cannot be searched reliably using binary search
    private static final String[] COUNTRY_CODE_TLDS = new String[] {
        "ac",                 // Ascension Island
        "ad",                 // Andorra
        "ae",                 // United Arab Emirates
        "af",                 // Afghanistan
        "ag",                 // Antigua and Barbuda
        "ai",                 // Anguilla
        "al",                 // Albania
        "am",                 // Armenia
//        "an",                 // Netherlands Antilles (retired)
        "ao",                 // Angola
        "aq",                 // Antarctica
        "ar",                 // Argentina
        "as",                 // American Samoa
        "at",                 // Austria
        "au",                 // Australia (includes Ashmore and Cartier Islands and Coral Sea Islands)
        "aw",                 // Aruba
        "ax",                 // Åland
        "az",                 // Azerbaijan
        "ba",                 // Bosnia and Herzegovina
        "bb",                 // Barbados
        "bd",                 // Bangladesh
        "be",                 // Belgium
        "bf",                 // Burkina Faso
        "bg",                 // Bulgaria
        "bh",                 // Bahrain
        "bi",                 // Burundi
        "bj",                 // Benin
        "bm",                 // Bermuda
        "bn",                 // Brunei Darussalam
        "bo",                 // Bolivia
        "br",                 // Brazil
        "bs",                 // Bahamas
        "bt",                 // Bhutan
        "bv",                 // Bouvet Island
        "bw",                 // Botswana
        "by",                 // Belarus
        "bz",                 // Belize
        "ca",                 // Canada
        "cc",                 // Cocos (Keeling) Islands
        "cd",                 // Democratic Republic of the Congo (formerly Zaire)
        "cf",                 // Central African Republic
        "cg",                 // Republic of the Congo
        "ch",                 // Switzerland
        "ci",                 // Côte d'Ivoire
        "ck",                 // Cook Islands
        "cl",                 // Chile
        "cm",                 // Cameroon
        "cn",                 // China, mainland
        "co",                 // Colombia
        "cr",                 // Costa Rica
        "cu",                 // Cuba
        "cv",                 // Cape Verde
        "cw",                 // Curaçao
        "cx",                 // Christmas Island
        "cy",                 // Cyprus
        "cz",                 // Czech Republic
        "de",                 // Germany
        "dj",                 // Djibouti
        "dk",                 // Denmark
        "dm",                 // Dominica
        "do",                 // Dominican Republic
        "dz",                 // Algeria
        "ec",                 // Ecuador
        "ee",                 // Estonia
        "eg",                 // Egypt
        "er",                 // Eritrea
        "es",                 // Spain
        "et",                 // Ethiopia
        "eu",                 // European Union
        "fi",                 // Finland
        "fj",                 // Fiji
        "fk",                 // Falkland Islands
        "fm",                 // Federated States of Micronesia
        "fo",                 // Faroe Islands
        "fr",                 // France
        "ga",                 // Gabon
        "gb",                 // Great Britain (United Kingdom)
        "gd",                 // Grenada
        "ge",                 // Georgia
        "gf",                 // French Guiana
        "gg",                 // Guernsey
        "gh",                 // Ghana
        "gi",                 // Gibraltar
        "gl",                 // Greenland
        "gm",                 // The Gambia
        "gn",                 // Guinea
        "gp",                 // Guadeloupe
        "gq",                 // Equatorial Guinea
        "gr",                 // Greece
        "gs",                 // South Georgia and the South Sandwich Islands
        "gt",                 // Guatemala
        "gu",                 // Guam
        "gw",                 // Guinea-Bissau
        "gy",                 // Guyana
        "hk",                 // Hong Kong
        "hm",                 // Heard Island and McDonald Islands
        "hn",                 // Honduras
        "hr",                 // Croatia (Hrvatska)
        "ht",                 // Haiti
        "hu",                 // Hungary
        "id",                 // Indonesia
        "ie",                 // Ireland (Éire)
        "il",                 // Israel
        "im",                 // Isle of Man
        "in",                 // India
        "io",                 // British Indian Ocean Territory
        "iq",                 // Iraq
        "ir",                 // Iran
        "is",                 // Iceland
        "it",                 // Italy
        "je",                 // Jersey
        "jm",                 // Jamaica
        "jo",                 // Jordan
        "jp",                 // Japan
        "ke",                 // Kenya
        "kg",                 // Kyrgyzstan
        "kh",                 // Cambodia (Khmer)
        "ki",                 // Kiribati
        "km",                 // Comoros
        "kn",                 // Saint Kitts and Nevis
        "kp",                 // North Korea
        "kr",                 // South Korea
        "kw",                 // Kuwait
        "ky",                 // Cayman Islands
        "kz",                 // Kazakhstan
        "la",                 // Laos (currently being marketed as the official domain for Los Angeles)
        "lb",                 // Lebanon
        "lc",                 // Saint Lucia
        "li",                 // Liechtenstein
        "lk",                 // Sri Lanka
        "lr",                 // Liberia
        "ls",                 // Lesotho
        "lt",                 // Lithuania
        "lu",                 // Luxembourg
        "lv",                 // Latvia
        "ly",                 // Libya
        "ma",                 // Morocco
        "mc",                 // Monaco
        "md",                 // Moldova
        "me",                 // Montenegro
        "mg",                 // Madagascar
        "mh",                 // Marshall Islands
        "mk",                 // Republic of Macedonia
        "ml",                 // Mali
        "mm",                 // Myanmar
        "mn",                 // Mongolia
        "mo",                 // Macau
        "mp",                 // Northern Mariana Islands
        "mq",                 // Martinique
        "mr",                 // Mauritania
        "ms",                 // Montserrat
        "mt",                 // Malta
        "mu",                 // Mauritius
        "mv",                 // Maldives
        "mw",                 // Malawi
        "mx",                 // Mexico
        "my",                 // Malaysia
        "mz",                 // Mozambique
        "na",                 // Namibia
        "nc",                 // New Caledonia
        "ne",                 // Niger
        "nf",                 // Norfolk Island
        "ng",                 // Nigeria
        "ni",                 // Nicaragua
        "nl",                 // Netherlands
        "no",                 // Norway
        "np",                 // Nepal
        "nr",                 // Nauru
        "nu",                 // Niue
        "nz",                 // New Zealand
        "om",                 // Oman
        "pa",                 // Panama
        "pe",                 // Peru
        "pf",                 // French Polynesia With Clipperton Island
        "pg",                 // Papua New Guinea
        "ph",                 // Philippines
        "pk",                 // Pakistan
        "pl",                 // Poland
        "pm",                 // Saint-Pierre and Miquelon
        "pn",                 // Pitcairn Islands
        "pr",                 // Puerto Rico
        "ps",                 // Palestinian territories (PA-controlled West Bank and Gaza Strip)
        "pt",                 // Portugal
        "pw",                 // Palau
        "py",                 // Paraguay
        "qa",                 // Qatar
        "re",                 // Réunion
        "ro",                 // Romania
        "rs",                 // Serbia
        "ru",                 // Russia
        "rw",                 // Rwanda
        "sa",                 // Saudi Arabia
        "sb",                 // Solomon Islands
        "sc",                 // Seychelles
        "sd",                 // Sudan
        "se",                 // Sweden
        "sg",                 // Singapore
        "sh",                 // Saint Helena
        "si",                 // Slovenia
        "sj",                 // Svalbard and Jan Mayen Islands Not in use (Norwegian dependencies; see .no)
        "sk",                 // Slovakia
        "sl",                 // Sierra Leone
        "sm",                 // San Marino
        "sn",                 // Senegal
        "so",                 // Somalia
        "sr",                 // Suriname
        "st",                 // São Tomé and Príncipe
        "su",                 // Soviet Union (deprecated)
        "sv",                 // El Salvador
        "sx",                 // Sint Maarten
        "sy",                 // Syria
        "sz",                 // Swaziland
        "tc",                 // Turks and Caicos Islands
        "td",                 // Chad
        "tf",                 // French Southern and Antarctic Lands
        "tg",                 // Togo
        "th",                 // Thailand
        "tj",                 // Tajikistan
        "tk",                 // Tokelau
        "tl",                 // East Timor (deprecated old code)
        "tm",                 // Turkmenistan
        "tn",                 // Tunisia
        "to",                 // Tonga
//        "tp",                 // East Timor (Retired)
        "tr",                 // Turkey
        "tt",                 // Trinidad and Tobago
        "tv",                 // Tuvalu
        "tw",                 // Taiwan, Republic of China
        "tz",                 // Tanzania
        "ua",                 // Ukraine
        "ug",                 // Uganda
        "uk",                 // United Kingdom
        "us",                 // United States of America
        "uy",                 // Uruguay
        "uz",                 // Uzbekistan
        "va",                 // Vatican City State
        "vc",                 // Saint Vincent and the Grenadines
        "ve",                 // Venezuela
        "vg",                 // British Virgin Islands
        "vi",                 // U.S. Virgin Islands
        "vn",                 // Vietnam
        "vu",                 // Vanuatu
        "wf",                 // Wallis and Futuna
        "ws",                 // Samoa (formerly Western Samoa)
        "xn--3e0b707e", // 한국 KISA (Korea Internet &amp; Security Agency)
        "xn--45brj9c", // ভারত National Internet Exchange of India
        "xn--80ao21a", // қаз Association of IT Companies of Kazakhstan
        "xn--90a3ac", // срб Serbian National Internet Domain Registry (RNIDS)
        "xn--90ais", // ??? Reliable Software Inc.
        "xn--clchc0ea0b2g2a9gcd", // சிங்கப்பூர் Singapore Network Information Centre (SGNIC) Pte Ltd
        "xn--d1alf", // мкд Macedonian Academic Research Network Skopje
        "xn--e1a4c", // ею EURid vzw/asbl
        "xn--fiqs8s", // 中国 China Internet Network Information Center
        "xn--fiqz9s", // 中國 China Internet Network Information Center
        "xn--fpcrj9c3d", // భారత్ National Internet Exchange of India
        "xn--fzc2c9e2c", // ලංකා LK Domain Registry
        "xn--gecrj9c", // ભારત National Internet Exchange of India
        "xn--h2brj9c", // भारत National Internet Exchange of India
        "xn--j1amh", // укр Ukrainian Network Information Centre (UANIC), Inc.
        "xn--j6w193g", // 香港 Hong Kong Internet Registration Corporation Ltd.
        "xn--kprw13d", // 台湾 Taiwan Network Information Center (TWNIC)
        "xn--kpry57d", // 台灣 Taiwan Network Information Center (TWNIC)
        "xn--l1acc", // мон Datacom Co.,Ltd
        "xn--lgbbat1ad8j", // الجزائر CERIST
        "xn--mgb9awbf", // عمان Telecommunications Regulatory Authority (TRA)
        "xn--mgba3a4f16a", // ایران Institute for Research in Fundamental Sciences (IPM)
        "xn--mgbaam7a8h", // امارات Telecommunications Regulatory Authority (TRA)
        "xn--mgbayh7gpa", // الاردن National Information Technology Center (NITC)
        "xn--mgbbh1a71e", // بھارت National Internet Exchange of India
        "xn--mgbc0a9azcg", // المغرب Agence Nationale de Réglementation des Télécommunications (ANRT)
        "xn--mgberp4a5d4ar", // السعودية Communications and Information Technology Commission
        "xn--mgbpl2fh", // ????? Sudan Internet Society
        "xn--mgbtx2b", // عراق Communications and Media Commission (CMC)
        "xn--mgbx4cd0ab", // مليسيا MYNIC Berhad
        "xn--mix891f", // 澳門 Bureau of Telecommunications Regulation (DSRT)
        "xn--node", // გე Information Technologies Development Center (ITDC)
        "xn--o3cw4h", // ไทย Thai Network Information Center Foundation
        "xn--ogbpf8fl", // سورية National Agency for Network Services (NANS)
        "xn--p1ai", // рф Coordination Center for TLD RU
        "xn--pgbs0dh", // تونس Agence Tunisienne d&#39;Internet
        "xn--qxam", // ελ ICS-FORTH GR
        "xn--s9brj9c", // ਭਾਰਤ National Internet Exchange of India
        "xn--wgbh1c", // مصر National Telecommunication Regulatory Authority - NTRA
        "xn--wgbl6a", // قطر Communications Regulatory Authority
        "xn--xkc2al3hye2a", // இலங்கை LK Domain Registry
        "xn--xkc2dl3a5ee0h", // இந்தியா National Internet Exchange of India
        "xn--y9a3aq", // ??? Internet Society
        "xn--yfro4i67o", // 新加坡 Singapore Network Information Centre (SGNIC) Pte Ltd
        "xn--ygbi2ammx", // فلسطين Ministry of Telecom &amp; Information Technology (MTIT)
        "ye",                 // Yemen
        "yt",                 // Mayotte
        "za",                 // South Africa
        "zm",                 // Zambia
        "zw",                 // Zimbabwe
    };

    // WARNING: this array MUST be sorted, otherwise it cannot be searched reliably using binary search
    private static final String[] LOCAL_TLDS = new String[] {
       "localdomain",         // Also widely used as localhost.localdomain
       "localhost",           // RFC2606 defined
    };

    // Additional arrays to supplement or override the built in ones.
    // The PLUS arrays are valid keys, the MINUS arrays are invalid keys

    /*
     * This field is used to detect whether the getInstance has been called.
     * After this, the method updateTLDOverride is not allowed to be called.
     * This field does not need to be volatile since it is only accessed from
     * synchronized methods.
     */
    private static boolean inUse = false;

    /*
     * These arrays are mutable, but they don't need to be volatile.
     * They can only be updated by the updateTLDOverride method, and any readers must get an instance
     * using the getInstance methods which are all (now) synchronised.
     */
    // WARNING: this array MUST be sorted, otherwise it cannot be searched reliably using binary search
    private static volatile String[] countryCodeTLDsPlus = EMPTY_STRING_ARRAY;

    // WARNING: this array MUST be sorted, otherwise it cannot be searched reliably using binary search
    private static volatile String[] genericTLDsPlus = EMPTY_STRING_ARRAY;

    // WARNING: this array MUST be sorted, otherwise it cannot be searched reliably using binary search
    private static volatile String[] countryCodeTLDsMinus = EMPTY_STRING_ARRAY;

    // WARNING: this array MUST be sorted, otherwise it cannot be searched reliably using binary search
    private static volatile String[] genericTLDsMinus = EMPTY_STRING_ARRAY;

    /**
     * enum used by {@link DomainValidator#updateTLDOverride(ArrayType, String[])}
     * to determine which override array to update / fetch
     * @since 1.5.0
     * @since 1.5.1 made public and added read-only array references
     */
    public enum ArrayType {
        /** Update (or get a copy of) the GENERIC_TLDS_PLUS table containing additonal generic TLDs */
        GENERIC_PLUS,
        /** Update (or get a copy of) the GENERIC_TLDS_MINUS table containing deleted generic TLDs */
        GENERIC_MINUS,
        /** Update (or get a copy of) the COUNTRY_CODE_TLDS_PLUS table containing additonal country code TLDs */
        COUNTRY_CODE_PLUS,
        /** Update (or get a copy of) the COUNTRY_CODE_TLDS_MINUS table containing deleted country code TLDs */
        COUNTRY_CODE_MINUS,
        /** Get a copy of the generic TLDS table */
        GENERIC_RO,
        /** Get a copy of the country code table */
        COUNTRY_CODE_RO,
        /** Get a copy of the infrastructure table */
        INFRASTRUCTURE_RO,
        /** Get a copy of the local table */
        LOCAL_RO
    }

    // For use by unit test code only
    static synchronized void clearTLDOverrides() {
        inUse = false;
        countryCodeTLDsPlus = EMPTY_STRING_ARRAY;
        countryCodeTLDsMinus = EMPTY_STRING_ARRAY;
        genericTLDsPlus = EMPTY_STRING_ARRAY;
        genericTLDsMinus = EMPTY_STRING_ARRAY;
    }

    /**
     * Update one of the TLD override arrays.
     * This must only be done at program startup, before any instances are accessed using getInstance.
     * <p>
     * For example:
     * <p>
     * <code>DomainValidator.updateTLDOverride(ArrayType.GENERIC_PLUS, new String[]{"apache"})}</code>
     * <p>
     * To clear an override array, provide an empty array.
     *
     * @param table the table to update, see {@link DomainValidator.ArrayType}
     * Must be one of the following
     * <ul>
     * <li>COUNTRY_CODE_MINUS</li>
     * <li>COUNTRY_CODE_PLUS</li>
     * <li>GENERIC_MINUS</li>
     * <li>GENERIC_PLUS</li>
     * </ul>
     * @param tlds the array of TLDs, must not be null
     * @throws IllegalStateException if the method is called after getInstance
     * @throws IllegalArgumentException if one of the read-only tables is requested
     * @since 1.5.0
     */
    public static synchronized void updateTLDOverride(ArrayType table, String[] tlds) {
        if (inUse) {
            throw new IllegalStateException("Can only invoke this method before calling getInstance");
        }
        String[] copy = new String[tlds.length];
        // Comparisons are always done with lower-case entries
        for (int i = 0; i < tlds.length; i++) {
            copy[i] = tlds[i].toLowerCase(Locale.ENGLISH);
        }
        Arrays.sort(copy);
        switch(table) {
        case COUNTRY_CODE_MINUS:
            countryCodeTLDsMinus = copy;
            break;
        case COUNTRY_CODE_PLUS:
            countryCodeTLDsPlus = copy;
            break;
        case GENERIC_MINUS:
            genericTLDsMinus = copy;
            break;
        case GENERIC_PLUS:
            genericTLDsPlus = copy;
            break;
        case COUNTRY_CODE_RO:
        case GENERIC_RO:
        case INFRASTRUCTURE_RO:
        case LOCAL_RO:
            throw new IllegalArgumentException("Cannot update the table: " + table);
        default:
            throw new IllegalArgumentException("Unexpected enum value: " + table);
        }
    }

    /**
     * Get a copy of the internal array.
     * @param table the array type (any of the enum values)
     * @return a copy of the array
     * @throws IllegalArgumentException if the table type is unexpected (should not happen)
     * @since 1.5.1
     */
    public static String[] getTLDEntries(ArrayType table) {
        final String[] array;
        switch(table) {
        case COUNTRY_CODE_MINUS:
            array = countryCodeTLDsMinus;
            break;
        case COUNTRY_CODE_PLUS:
            array = countryCodeTLDsPlus;
            break;
        case GENERIC_MINUS:
            array = genericTLDsMinus;
            break;
        case GENERIC_PLUS:
            array = genericTLDsPlus;
            break;
        case GENERIC_RO:
            array = GENERIC_TLDS;
            break;
        case COUNTRY_CODE_RO:
            array = COUNTRY_CODE_TLDS;
            break;
        case INFRASTRUCTURE_RO:
            array = INFRASTRUCTURE_TLDS;
            break;
        case LOCAL_RO:
            array = LOCAL_TLDS;
            break;
        default:
            throw new IllegalArgumentException("Unexpected enum value: " + table);
        }
        return Arrays.copyOf(array, array.length); // clone the array
    }

    /**
     * Converts potentially Unicode input to punycode.
     * If conversion fails, returns the original input.
     *
     * @param input the string to convert, not null
     * @return converted input, or original input if conversion fails
     */
    // Needed by UrlValidator
    static String unicodeToASCII(String input) {
        if (isOnlyASCII(input)) { // skip possibly expensive processing
            return input;
        }
        try {
            final String ascii = IDN.toASCII(input);
            if (IDNBUGHOLDER.IDN_TOASCII_PRESERVES_TRAILING_DOTS) {
                return ascii;
            }
            final int length = input.length();
            if (length == 0) { // check there is a last character
                return input;
            }
            // RFC3490 3.1. 1)
            //            Whenever dots are used as label separators, the following
            //            characters MUST be recognized as dots: U+002E (full stop), U+3002
            //            (ideographic full stop), U+FF0E (fullwidth full stop), U+FF61
            //            (halfwidth ideographic full stop).
            char lastChar = input.charAt(length-1); // fetch original last char
            switch(lastChar) {
                case '\u002E': // "." full stop
                case '\u3002': // ideographic full stop
                case '\uFF0E': // fullwidth full stop
                case '\uFF61': // halfwidth ideographic full stop
                    return ascii + "."; // restore the missing stop
                default:
                    return ascii;
            }
        } catch (IllegalArgumentException e) { // input is not valid
            return input;
        }
    }

    private static class IDNBUGHOLDER {
        private static boolean keepsTrailingDot() {
            final String input = "a."; // must be a valid name
            return input.equals(IDN.toASCII(input));
        }

        private static final boolean IDN_TOASCII_PRESERVES_TRAILING_DOTS = keepsTrailingDot();
    }

    /*
     * Check if input contains only ASCII
     * Treats null as all ASCII
     */
    private static boolean isOnlyASCII(String input) {
        if (input == null) {
            return true;
        }
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) > 0x7F) { // CHECKSTYLE IGNORE MagicNumber
                return false;
            }
        }
        return true;
    }

    /**
     * Check if a sorted array contains the specified key
     *
     * @param sortedArray the array to search
     * @param key the key to find
     * @return {@code true} if the array contains the key
     */
    private static boolean arrayContains(String[] sortedArray, String key) {
        return Arrays.binarySearch(sortedArray, key) >= 0;
    }
}
