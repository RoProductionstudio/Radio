package com.roproduction.radio.data

/**
 * Встроенный список российских радиостанций.
 *
 * Адреса потоков иногда меняются. Если прямая ссылка перестала работать, приложение само
 * найдёт актуальный поток в открытом каталоге radio-browser.info и запомнит его.
 */
object DefaultStations {
    private const val VGTRK = "https://icecast-vgtrk.cdnvideo.ru"

    val all: List<Station> = listOf(
        Station(
            id = "vesti_fm",
            name = "Вести ФМ",
            description = "Новости и аналитика · 97.6 FM Москва",
            streamUrl = "$VGTRK/vestifm_mp3_192kbps",
            searchQueries = listOf("Вести ФМ", "Vesti FM"),
        ),
        Station(
            id = "kp",
            name = "Комсомольская правда",
            description = "Новости, разговорное радио · 97.2 FM Москва",
            streamUrl = "https://kpradio.hostingradio.ru:8000/128",
            searchQueries = listOf("Комсомольская правда", "Радио КП", "Komsomolskaya Pravda"),
        ),
        Station(
            id = "mayak",
            name = "Маяк",
            description = "Разговорное радио · 103.4 FM Москва, 106.6 FM Томск",
            streamUrl = "$VGTRK/mayakfm_mp3_192kbps",
            searchQueries = listOf("Маяк", "Radio Mayak"),
        ),
        Station(
            id = "tomsk_news",
            name = "Томские новости",
            description = "Томск · поток ищется в каталоге станций",
            streamUrl = "",
            searchQueries = listOf("Томские новости", "Tomskie Novosti", "Томск новости"),
        ),
        Station(
            id = "radio_sibir",
            name = "Радио Сибирь",
            description = "Томск · 104.6 FM",
            streamUrl = "",
            searchQueries = listOf("Радио Сибирь", "Radio Sibir"),
        ),
        Station(
            id = "radio_rossii",
            name = "Радио России",
            description = "Федеральное и региональное вещание · 102.9 FM Томск",
            streamUrl = "$VGTRK/rrzonam_mp3_192kbps",
            searchQueries = listOf("Радио России", "Radio Rossii"),
        ),
        Station(
            id = "kultura",
            name = "Радио Культура",
            description = "Культура, литература, классика",
            streamUrl = "$VGTRK/kulturafm_mp3_192kbps",
            searchQueries = listOf("Радио Культура", "Radio Kultura"),
        ),
        Station(
            id = "bfm",
            name = "Business FM",
            description = "Деловые новости",
            streamUrl = "https://bfm.hostingradio.ru/fm",
            searchQueries = listOf("Business FM", "Бизнес ФМ"),
        ),
        Station(
            id = "zvezda",
            name = "Радио Звезда",
            description = "Новости и музыка",
            streamUrl = "",
            searchQueries = listOf("Радио Звезда", "Radio Zvezda"),
        ),
        Station(
            id = "govorit_moskva",
            name = "Говорит Москва",
            description = "Новости и разговорное радио",
            streamUrl = "",
            searchQueries = listOf("Говорит Москва", "Govorit Moskva"),
        ),
        Station(
            id = "europa_plus",
            name = "Европа Плюс",
            description = "Поп-музыка",
            streamUrl = "https://ep256.hostingradio.ru:8052/europaplus256.mp3",
            searchQueries = listOf("Европа Плюс", "Europa Plus"),
        ),
        Station(
            id = "rusradio",
            name = "Русское Радио",
            description = "Русская поп-музыка",
            streamUrl = "https://rusradio.hostingradio.ru/rusradio128.mp3",
            searchQueries = listOf("Русское Радио", "Russkoe Radio"),
        ),
        Station(
            id = "retro",
            name = "Ретро FM",
            description = "Хиты 70-х, 80-х, 90-х",
            streamUrl = "https://retro.hostingradio.ru:8043/retro256.mp3",
            searchQueries = listOf("Ретро FM", "Retro FM"),
        ),
        Station(
            id = "avtoradio",
            name = "Авторадио",
            description = "Музыка в дорогу",
            streamUrl = "https://pub0302.101.ru:8443/stream/air/aac/64/100",
            searchQueries = listOf("Авторадио", "Avtoradio"),
        ),
        Station(
            id = "dorozhnoe",
            name = "Дорожное радио",
            description = "Русская музыка",
            streamUrl = "https://dorognoe.hostingradio.ru:8000/dorognoe",
            searchQueries = listOf("Дорожное радио", "Dorozhnoe"),
        ),
        Station(
            id = "shanson",
            name = "Радио Шансон",
            description = "Шансон",
            streamUrl = "https://chanson.hostingradio.ru:8041/chanson256.mp3",
            searchQueries = listOf("Радио Шансон", "Shanson"),
        ),
        Station(
            id = "nashe",
            name = "Наше Радио",
            description = "Русский рок",
            streamUrl = "https://nashe1.hostingradio.ru/nashe-256",
            searchQueries = listOf("Наше Радио", "Nashe Radio"),
        ),
        Station(
            id = "energy",
            name = "Радио Energy",
            description = "Танцевальная музыка",
            streamUrl = "https://pub0302.101.ru:8443/stream/air/aac/64/99",
            searchQueries = listOf("Energy", "NRJ Russia"),
        ),
        Station(
            id = "relax",
            name = "Relax FM",
            description = "Спокойная музыка",
            streamUrl = "https://pub0302.101.ru:8443/stream/air/aac/64/200",
            searchQueries = listOf("Relax FM"),
        ),
        Station(
            id = "humor",
            name = "Юмор FM",
            description = "Юмор и музыка",
            streamUrl = "https://pub0302.101.ru:8443/stream/air/aac/64/102",
            searchQueries = listOf("Юмор FM", "Humor FM"),
        ),
        Station(
            id = "dfm",
            name = "DFM",
            description = "Дискотека · 103.8 FM Томск",
            streamUrl = "https://dfm.hostingradio.ru/dfm96.aacp",
            searchQueries = listOf("DFM"),
        ),
        Station(
            id = "dacha",
            name = "Радио Дача",
            description = "Весёлая музыка",
            streamUrl = "",
            searchQueries = listOf("Радио Дача", "Radio Dacha"),
        ),
    )
}
