package com.example.advisor.school;

public enum Municipality {
    AGEO_CITY("上尾市"),
    ASAKA_CITY("朝霞市"),
    SAITAMA_CITY("さいたま市"),
    IRUMA_CITY("入間市"),
    OGANO_TOWN("小鹿野町"),
    OGAWA_TOWN("小川町"),
    OKEGAWA_CITY("桶川市"),
    OGOSE_TOWN("越生町"),
    KASUKABE_CITY("春日部市"),
    KAWAGUCHI_CITY("川口市"),
    KAZO_CITY("加須市"),
    KAMIKAWA_TOWN("神川町"),
    KAMISATO_TOWN("上里町"),
    KAWAGOE_CITY("川越市"),
    KAWAJIMA_TOWN("川島町"),
    KITAMOTO_CITY("北本市"),
    GYODA_CITY("行田市"),
    KUKI_CITY("久喜市"),
    KUMAGAYA_CITY("熊谷市"),
    KONOSU_CITY("鴻巣市"),
    KOSHIGAYA_CITY("越谷市"),
    SAKADO_CITY("坂戸市"),
    SATTE_CITY("幸手市"),
    SAYAMA_CITY("狭山市"),
    SHIKI_CITY("志木市"),
    SHIRAOKA_CITY("白岡市"),
    SUGITO_TOWN("杉戸町"),
    SOKA_CITY("草加市"),
    CHICHIBU_CITY("秩父市"),
    TSURUGASHIMA_CITY("鶴ヶ島市"),
    TOKIGAWA_TOWN("ときがわ町"),
    TOKOROZAWA_CITY("所沢市"),
    TODA_CITY("戸田市"),
    NAGATORO_TOWN("長瀞町"),
    NAMEGAWA_TOWN("滑川町"),
    NIIZA_CITY("新座市"),
    HASUDA_CITY("蓮田市"),
    HATOYAMA_TOWN("鳩山町"),
    HANYU_CITY("羽生市"),
    HANNO_CITY("飯能市"),
    HIGASHICHICHIBU_VILLAGE("東秩父村"),
    HIGASHIMATSUYAMA_CITY("東松山市"),
    HIDAKA_CITY("日高市"),
    FUKAYA_CITY("深谷市"),
    FUJIMI_CITY("富士見市"),
    FUJIMINO_CITY("ふじみ野市"),
    HONJO_CITY("本庄市"),
    MATSUBUSHI_TOWN("松伏町"),
    MISATO_CITY("三郷市"),
    MISATO_TOWN("美里町"),
    MINANO_TOWN("皆野町"),
    MIYASHIRO_TOWN("宮代町"),
    MIYOSHI_TOWN("三芳町"),
    MOROYAMA_TOWN("毛呂山町"),
    YASHIO_CITY("八潮市"),
    YOKOZE_TOWN("横瀬町"),
    YOSHIKAWA_CITY("吉川市"),
    YOSHIMI_TOWN("吉見町"),
    YORII_TOWN("寄居町"),
    RANZAN_TOWN("嵐山町"),
    WAKO_CITY("和光市"),
    WARABI_CITY("蕨市"),
    INA_TOWN("伊奈町"),
    ;

    private final String label;

    Municipality(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
