from nltk.corpus import stopwords
import pandas as pd
from wordcloud import WordCloud
import matplotlib.pyplot as plt
import numpy as np
from nltk.corpus import stopwords
import cleantext


def getwords_content_pre(df: pd.DataFrame, filename: str, german_stop_words: list[str]) -> None:

    df = df.copy()
    # lösche alle Beiträge > 2020
    for index, row in df.iterrows():
        if int(row["YEAR"]) > 2020:
            df.drop(index, inplace=True)
    df.dropna(inplace=True)

    text_list = df["CONTENT"].tolist()
    text = " ".join(text_list)
    text = text.lower()
    text = cleantext.clean(text, extra_spaces=True,
                           lowercase=True, numbers=True, punct=True)

    text_list = [i for i in text.split() if i not in german_stop_words]

    text = " ".join(text_list)

    # erstelle wordcloud
    word_cloud = WordCloud(
        collocations=False, background_color='white').generate(text)
    # speichere wordcloud
    word_cloud.to_file(filename+"_wordcloud_content_pre_covid"+".png")


def getwords_content_post(df: pd.DataFrame, filename: str, german_stop_words: list[str]) -> None:

    print(german_stop_words)
    df = df.copy()

    # lösche pre covid daten
    for index, row in df.iterrows():
        if int(row["YEAR"]) < 2021:
            df.drop(index, inplace=True)

    df.dropna(inplace=True)

    text_list = df["CONTENT"].tolist()
    text = " ".join(text_list)
    text = text.lower()

    text = cleantext.clean(text, extra_spaces=True,
                           lowercase=True, numbers=True, punct=True)
    text_list = [i for i in text.split() if i not in german_stop_words]
    text = " ".join(text_list)

    # erstelle wordcloud
    word_cloud = WordCloud(
        collocations=False, background_color='white').generate(text)
    # speichere wordcloud
    word_cloud.to_file(filename+"_wordcloud_content_post_covid"+".png")


def getwords_headline_pre(df: pd.DataFrame, filename: str, german_stop_words: list[str]) -> None:

    df = df.copy()

    for index, row in df.iterrows():
        if int(row["YEAR"]) > 2020:
            df.drop(index, inplace=True)
    df.dropna(inplace=True)

    text_list = df["HEADLINE"].tolist()
    text = " ".join(text_list)
    text = text.lower()
    text = cleantext.clean(text, extra_spaces=True,
                           lowercase=True, numbers=True, punct=True)
    text_list = [i for i in text.split() if i not in german_stop_words]

    # print(german_stop_words)
    text = " ".join(text_list)

    word_cloud = WordCloud(
        collocations=False, background_color='white').generate(text)

    word_cloud.to_file(filename+"_wordcloud_überschriften_pre_covid"+".png")


def getwords_headline_post(df: pd.DataFrame, filename: str, german_stop_words: list[str]) -> None:

    df = df.copy()

    for index, row in df.iterrows():
        if int(row["YEAR"]) < 2021:
            df.drop(index, inplace=True)
    df.dropna(inplace=True)

    text_list = df["HEADLINE"].tolist()
    text = " ".join(text_list)
    text = text.lower()

    text = cleantext.clean(text, extra_spaces=True,
                           lowercase=True, numbers=True, punct=True)
    text_list = [i for i in text.split() if i.strip() not in german_stop_words]
    text = " ".join(text_list)

    word_cloud = WordCloud(
        collocations=False, background_color='white').generate(text)

    word_cloud.to_file(filename+"_wordcloud_überschriften_post_covid"+".png")


def get_wordcloud(inputfiles: list[str], name: str or None = None, ignore_words: list[str] = ["Spiegel, FAZ", "Bild", "Die", "Wir", "wir", "werden", "die", "Die",
                                                                                              "immer", "müssen", "geht", "da", "sei", "mehr", "mal", "wurde", "neue", "gibt", "wegen", "sogar", "seit", "lassen",
                                                                                              "sagte", "viele", "schon", "warum", "worden", "zwei", "sagt", "dabei",
                                                                                              "neuen", "sollen", "freitag", "ersten", "land", "rund", "wurden", "drei",
                                                                                              "etwa", "hatten", "seien", "erst", "weitere", "jahr", "jahre", "beim", "ende",
                                                                                              "hätten", "angaben", "zunächst", "wer", "inhalt", "wäre", "dabei", "worden", "foto",
                                                                                              "vergangenen", "bereit", "mann", "macht", "zurück", "jähriger", "welt", "kommentar",
                                                                                              "haus", "leben", "gut", "jahren", "montag", "zudem", "mittlerweile", "zufolge", "jährige",
                                                                                              "fall", "allerdings", "ab", "sowie", "dafür", "beiden", "bereit", "teilte", "dpa",
                                                                                              "könnten", "könne", "fünf", "dienstag", "prozent", "spiegel", "jahren", "steht",
                                                                                              "dpainfocom"]) -> None:

    # print(ignore_words)
    # namen erstellen um file zu speichern
    if name is None:
        starts = [file.split('/')[-1][:3] for file in inputfiles]
    name = f"{'_'.join([s for s in starts])}"

    # alle dataframes in ein großes dataframe umwandeln
    df = pd.read_excel(inputfiles[0], nrows=3000)
    for file in inputfiles[1:]:
        df = df.append(pd.read_excel(file, nrows=3000), ignore_index=True)

    # umwaldung der daten von strings zu datetime
    df['DATE'] = pd.to_datetime(df['DATE'])
    df['YEAR'] = df['DATE'].dt.year

    # erweitere stopwords um ignore_words
    german_stop_words = list(stopwords.words('german'))+ignore_words
    for i in range(len(german_stop_words)):
        german_stop_words[i] = german_stop_words[i].lower()

    # erstelle clouds
    getwords_content_pre(df, name, german_stop_words)
    getwords_content_post(df, name, german_stop_words)
    getwords_headline_pre(df, name, german_stop_words)
    getwords_headline_post(df, name, german_stop_words)


if __name__ == "__main__":

    get_wordcloud(inputfiles=[
        "FAZ_SCRAPING_BEREINIGT_12.07.2022.xlsx",
        "SPIEGEL_SCRAPING_BEREINIGT_12.07.2022.xlsx",
        "BILD_SCRAPING_BEREINIGT_12.07.2022.xlsx",
        "SUEDDEUTSCHE_SCRAPING_BEREINIGT_12.07.2022.xlsx"
    ])
