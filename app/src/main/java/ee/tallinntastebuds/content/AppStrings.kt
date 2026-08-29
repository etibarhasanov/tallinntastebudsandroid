package ee.tallinntastebuds.content

/**
 * The short list of words the app needs that the website has no equivalent for
 * — tab names, the saved list, the about screen.
 *
 * Everything a reader sees about a *place* comes from `ui.json` and changes when
 * the site changes. These are the app's own furniture, so they live here, in the
 * same nine languages the site offers. Keep the language set in step with
 * `data/ui.json`; anything missing falls back to English.
 */
object AppStrings {

    enum class Key {
        TAB_MAP, TAB_LIST, TAB_SAVED,
        SORT, SORT_NEAREST, SAVE, SAVED, SAVED_EMPTY, SAVED_EMPTY_HINT,
        APPEARANCE, STYLE_RED, STYLE_PINK,
        DISCOUNT_OFFER, DISCOUNT_OPEN, ABOUT, ABOUT_BODY, OPEN_WEBSITE, SYNC_NOTE,
    }

    fun text(key: Key, lang: String): String =
        table[lang]?.get(key) ?: table["en"]?.get(key) ?: key.name

    private val table: Map<String, Map<Key, String>> = mapOf(
        "en" to mapOf(
            Key.TAB_MAP to "Map", Key.TAB_LIST to "Places", Key.TAB_SAVED to "Saved",
            Key.SORT to "Sort", Key.SORT_NEAREST to "Nearest",
            Key.SAVE to "Save", Key.SAVED to "Saved",
            Key.SAVED_EMPTY to "Nothing saved yet.",
            Key.SAVED_EMPTY_HINT to "Tap the bookmark on a place to keep it here.",
            Key.APPEARANCE to "Appearance",
            Key.STYLE_RED to "Red", Key.STYLE_PINK to "Pink (dark)",
            Key.DISCOUNT_OFFER to "Discount", Key.DISCOUNT_OPEN to "Get the code",
            Key.ABOUT to "About", Key.OPEN_WEBSITE to "Open the website",
            Key.ABOUT_BODY to "Every place on this map has been visited and approved in person. There are no scores: being on the map is the verdict.",
            Key.SYNC_NOTE to "Places, text and photos come from tallinntastebuds.ee, so the app shows whatever the website shows.",
        ),
        "et" to mapOf(
            Key.TAB_MAP to "Kaart", Key.TAB_LIST to "Kohad", Key.TAB_SAVED to "Salvestatud",
            Key.SORT to "Järjesta", Key.SORT_NEAREST to "Lähim",
            Key.SAVE to "Salvesta", Key.SAVED to "Salvestatud",
            Key.SAVED_EMPTY to "Midagi pole veel salvestatud.",
            Key.SAVED_EMPTY_HINT to "Puuduta koha juures järjehoidjat, et see siia jääks.",
            Key.APPEARANCE to "Välimus",
            Key.STYLE_RED to "Punane", Key.STYLE_PINK to "Roosa (tume)",
            Key.DISCOUNT_OFFER to "Soodustus", Key.DISCOUNT_OPEN to "Võta kood",
            Key.ABOUT to "Teave", Key.OPEN_WEBSITE to "Ava veebileht",
            Key.ABOUT_BODY to "Igas selle kaardi kohas olen ise käinud ja selle heaks kiitnud. Hindeid pole: kaardil olemine ongi hinnang.",
            Key.SYNC_NOTE to "Kohad, tekstid ja pildid tulevad lehelt tallinntastebuds.ee, seega rakendus näitab sedasama, mida veebileht.",
        ),
        "ru" to mapOf(
            Key.TAB_MAP to "Карта", Key.TAB_LIST to "Места", Key.TAB_SAVED to "Сохранённое",
            Key.SORT to "Сортировка", Key.SORT_NEAREST to "Ближайшие",
            Key.SAVE to "Сохранить", Key.SAVED to "Сохранено",
            Key.SAVED_EMPTY to "Пока ничего не сохранено.",
            Key.SAVED_EMPTY_HINT to "Нажмите закладку у места, чтобы оно осталось здесь.",
            Key.APPEARANCE to "Оформление",
            Key.STYLE_RED to "Красный", Key.STYLE_PINK to "Розовый (тёмный)",
            Key.DISCOUNT_OFFER to "Скидка", Key.DISCOUNT_OPEN to "Получить код",
            Key.ABOUT to "О приложении", Key.OPEN_WEBSITE to "Открыть сайт",
            Key.ABOUT_BODY to "В каждом месте на этой карте я был лично и одобрил его. Оценок нет: попадание на карту и есть оценка.",
            Key.SYNC_NOTE to "Места, тексты и фотографии берутся с tallinntastebuds.ee, поэтому приложение показывает то же, что и сайт.",
        ),
        "uk" to mapOf(
            Key.TAB_MAP to "Карта", Key.TAB_LIST to "Місця", Key.TAB_SAVED to "Збережені",
            Key.SORT to "Сортувати", Key.SORT_NEAREST to "Найближчі",
            Key.SAVE to "Зберегти", Key.SAVED to "Збережено",
            Key.SAVED_EMPTY to "Ще нічого не збережено.",
            Key.SAVED_EMPTY_HINT to "Торкніться закладки біля місця, щоб воно лишилося тут.",
            Key.APPEARANCE to "Вигляд",
            Key.STYLE_RED to "Червоний", Key.STYLE_PINK to "Рожевий (темний)",
            Key.DISCOUNT_OFFER to "Знижка", Key.DISCOUNT_OPEN to "Отримати код",
            Key.ABOUT to "Про застосунок", Key.OPEN_WEBSITE to "Відкрити сайт",
            Key.ABOUT_BODY to "У кожному місці на цій карті я був особисто і схвалив його. Оцінок немає: потрапити на карту — це вже вирок.",
            Key.SYNC_NOTE to "Місця, тексти та фото беруться з tallinntastebuds.ee, тому застосунок показує те саме, що й сайт.",
        ),
        "fi" to mapOf(
            Key.TAB_MAP to "Kartta", Key.TAB_LIST to "Paikat", Key.TAB_SAVED to "Tallennetut",
            Key.SORT to "Järjestä", Key.SORT_NEAREST to "Lähin",
            Key.SAVE to "Tallenna", Key.SAVED to "Tallennettu",
            Key.SAVED_EMPTY to "Mitään ei ole vielä tallennettu.",
            Key.SAVED_EMPTY_HINT to "Napauta paikan kirjanmerkkiä, niin se jää tänne.",
            Key.APPEARANCE to "Ulkoasu",
            Key.STYLE_RED to "Punainen", Key.STYLE_PINK to "Pinkki (tumma)",
            Key.DISCOUNT_OFFER to "Alennus", Key.DISCOUNT_OPEN to "Hae koodi",
            Key.ABOUT to "Tietoja", Key.OPEN_WEBSITE to "Avaa sivusto",
            Key.ABOUT_BODY to "Olen käynyt jokaisessa tämän kartan paikassa itse ja hyväksynyt sen. Pisteitä ei ole: kartalla oleminen on tuomio.",
            Key.SYNC_NOTE to "Paikat, tekstit ja kuvat tulevat osoitteesta tallinntastebuds.ee, joten sovellus näyttää saman kuin sivusto.",
        ),
        "az" to mapOf(
            Key.TAB_MAP to "Xəritə", Key.TAB_LIST to "Yerlər", Key.TAB_SAVED to "Yadda saxlanan",
            Key.SORT to "Sırala", Key.SORT_NEAREST to "Ən yaxın",
            Key.SAVE to "Yadda saxla", Key.SAVED to "Saxlanıb",
            Key.SAVED_EMPTY to "Hələ heç nə saxlanmayıb.",
            Key.SAVED_EMPTY_HINT to "Yerin yanındakı əlfəcinə toxun ki, burada qalsın.",
            Key.APPEARANCE to "Görünüş",
            Key.STYLE_RED to "Qırmızı", Key.STYLE_PINK to "Çəhrayı (tünd)",
            Key.DISCOUNT_OFFER to "Endirim", Key.DISCOUNT_OPEN to "Kodu al",
            Key.ABOUT to "Haqqında", Key.OPEN_WEBSITE to "Saytı aç",
            Key.ABOUT_BODY to "Bu xəritədəki hər yerdə özüm olmuşam və bəyənmişəm. Bal yoxdur: xəritədə olmaq elə qiymətdir.",
            Key.SYNC_NOTE to "Yerlər, mətnlər və şəkillər tallinntastebuds.ee saytından gəlir, ona görə tətbiq saytda nə varsa onu göstərir.",
        ),
        "pt" to mapOf(
            Key.TAB_MAP to "Mapa", Key.TAB_LIST to "Lugares", Key.TAB_SAVED to "Guardados",
            Key.SORT to "Ordenar", Key.SORT_NEAREST to "Mais perto",
            Key.SAVE to "Guardar", Key.SAVED to "Guardado",
            Key.SAVED_EMPTY to "Ainda não guardou nada.",
            Key.SAVED_EMPTY_HINT to "Toque no marcador de um lugar para o manter aqui.",
            Key.APPEARANCE to "Aspeto",
            Key.STYLE_RED to "Vermelho", Key.STYLE_PINK to "Rosa (escuro)",
            Key.DISCOUNT_OFFER to "Desconto", Key.DISCOUNT_OPEN to "Obter o código",
            Key.ABOUT to "Sobre", Key.OPEN_WEBSITE to "Abrir o site",
            Key.ABOUT_BODY to "Estive pessoalmente em todos os lugares deste mapa e aprovei-os. Não há pontuações: estar no mapa é o veredicto.",
            Key.SYNC_NOTE to "Os lugares, os textos e as fotos vêm de tallinntastebuds.ee, por isso a aplicação mostra o mesmo que o site.",
        ),
        "es" to mapOf(
            Key.TAB_MAP to "Mapa", Key.TAB_LIST to "Sitios", Key.TAB_SAVED to "Guardados",
            Key.SORT to "Ordenar", Key.SORT_NEAREST to "Más cerca",
            Key.SAVE to "Guardar", Key.SAVED to "Guardado",
            Key.SAVED_EMPTY to "Aún no has guardado nada.",
            Key.SAVED_EMPTY_HINT to "Toca el marcador de un sitio para que se quede aquí.",
            Key.APPEARANCE to "Apariencia",
            Key.STYLE_RED to "Rojo", Key.STYLE_PINK to "Rosa (oscuro)",
            Key.DISCOUNT_OFFER to "Descuento", Key.DISCOUNT_OPEN to "Conseguir el código",
            Key.ABOUT to "Acerca de", Key.OPEN_WEBSITE to "Abrir la web",
            Key.ABOUT_BODY to "He estado en persona en todos los sitios de este mapa y los he aprobado. No hay puntuaciones: estar en el mapa es el veredicto.",
            Key.SYNC_NOTE to "Los sitios, los textos y las fotos vienen de tallinntastebuds.ee, así que la app muestra lo mismo que la web.",
        ),
        "tr" to mapOf(
            Key.TAB_MAP to "Harita", Key.TAB_LIST to "Mekânlar", Key.TAB_SAVED to "Kaydedilenler",
            Key.SORT to "Sırala", Key.SORT_NEAREST to "En yakın",
            Key.SAVE to "Kaydet", Key.SAVED to "Kaydedildi",
            Key.SAVED_EMPTY to "Henüz bir şey kaydedilmedi.",
            Key.SAVED_EMPTY_HINT to "Burada kalması için mekânın yer imine dokunun.",
            Key.APPEARANCE to "Görünüm",
            Key.STYLE_RED to "Kırmızı", Key.STYLE_PINK to "Pembe (koyu)",
            Key.DISCOUNT_OFFER to "İndirim", Key.DISCOUNT_OPEN to "Kodu al",
            Key.ABOUT to "Hakkında", Key.OPEN_WEBSITE to "Siteyi aç",
            Key.ABOUT_BODY to "Bu haritadaki her mekâna kendim gittim ve onayladım. Puan yok: haritada olmak zaten karardır.",
            Key.SYNC_NOTE to "Mekânlar, metinler ve fotoğraflar tallinntastebuds.ee adresinden gelir; yani uygulama sitede ne varsa onu gösterir.",
        ),
    )
}
