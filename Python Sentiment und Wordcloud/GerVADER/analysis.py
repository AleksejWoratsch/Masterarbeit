import pandas as pd
from vaderSentimentGER import SentimentIntensityAnalyzer
import tqdm
import matplotlib.pyplot as plt
import numpy as np
import matplotlib as mpl
from latex import *

label_size = 12
mpl.rcParams['xtick.labelsize'] = label_size


def format_plot(x: None or list = None, y: list = None,
                title: str = None, x_label: str = None,
                y_label: str = None, name: str = None,
                x_line: bool = False,
                ):
    """Args:
        x (liste, optional): x values des plots, default auf none
        y (liste, optional): y values des plots, default auf none
        title (string, optional): titel des plots, default auf none
        x_label (str, optional): x achsen label, default none
        y_label (str, optional): y achsen label, default none
        name (str, optional): name des files in der der plot gespeichert werden soll, default none
        x_line = param. für vertikale Linie
    """
    # /7 = es gibt für jeden Tag daten
    # date_list = ["woche "+str(i//7) for i in range(len(y))
    #              ]
    plt.clf()
    plt.title(title)
    plt.xlabel(x_label)
    plt.ylabel(y_label)
    # plt.axvline(x=x_line, color="red")

    # plt.xticks(rotation=90)
    plt.xticks([])

    # plt.xticks(ticks=np.arange(len(y))[::28],
    #            # labels=date_list[::28]
    #            )
    # plt.xticks(fontsize=8)
    if x is not None:
        plt.plot(x, y)
    else:
        plt.plot(y)
    if x_line == True:
        plt.axvline(x=float(((len(y)) / 100) * 55), color="red")
    # vertikale Linie bei 55% der Timeline, zur Unterteilung von Pre und Post Covid, 55% entsprechen dem ersten Zeitraum
    plt.savefig(f"{name}.png", bbox_inches='tight')
    # plt.xticks(rotation=0)
    plt.clf()


def getrolling_content_pre(df: pd.DataFrame, filename: str) -> None:

    # daten vor covid für content
    df = df[df['ERA'] == "pre"].copy()
    # wandle in Datetime um, sortiere anschließend, um anschließend ein 28-Tage intervall zu bilden
    df["DATE"] = pd.to_datetime(df["DATE"])
    df = df.sort_values('DATE')
    df1 = df[["DATE", "PUBLISHER"]]

    # groupby
    res = df1.groupby(['DATE']).value_counts(normalize=False)
    res = res.reset_index()
    res.columns = ['DATE', 'PUBLISHER', 'COUNTS']
    average_per_day = res['COUNTS'].mean()

    # wähle Datum und sentiment score
    df1 = df[["DATE", "CONTENT_SENTIMENT_SCORE"]]

    # durchschnittsbildung über mean
    # https://pandas.pydata.org/docs/reference/api/pandas.core.window.rolling.Rolling.mean.html

    res = df1.groupby('DATE', as_index=False, sort=False)[
        'CONTENT_SENTIMENT_SCORE'].mean()
    res['rolling_mean'] = res['CONTENT_SENTIMENT_SCORE'].rolling(28).mean()
    avg_sentiment_list = [0]+res["rolling_mean"].tolist()[1:]

    title = "Durchschnittliches Sentiment vor Covid \n für den gesamten Content"
    name = f"{filename}_sentiment_content_prevcovid"
    # erstellt und speichert plot
    format_plot(y=avg_sentiment_list, title=title,
                x_label="Zeit", y_label="Score", name=name)

    return average_per_day


def getrolling_content_post(df: pd.DataFrame, filename: str) -> None:

    # post covid content
    df = df[df['ERA'] == "post"].copy()
    df["DATE"] = pd.to_datetime(df["DATE"])
    df = df.sort_values('DATE')
    df1 = df[["DATE", "PUBLISHER"]]

    # group by
    res = df1.groupby(['DATE']).value_counts(normalize=False)
    res = res.reset_index()
    res.columns = ['DATE', 'PUBLISHER', 'COUNTS']
    average_per_day = res['COUNTS'].mean()

    # datum und sentimentscore auwählen
    df1 = df[["DATE", "CONTENT_SENTIMENT_SCORE"]]

    # durchschnittsbildung über mean
    res = df1.groupby('DATE', as_index=False, sort=False)[
        'CONTENT_SENTIMENT_SCORE'].mean()
    res['rolling_mean'] = res['CONTENT_SENTIMENT_SCORE'].rolling(28).mean()
    avg_sentiment_list = [0]+res["rolling_mean"].tolist()[1:]
    # avg_sentiment_list=avg_sentiment_list

    # plott avg. Sentiment
    title = "Durchschnittliches Sentiment nach Covid \n für den gesamten Content"
    name = f"{filename}_sentiment_content_postcovid"
    format_plot(y=avg_sentiment_list, title=title,
                x_label="Zeit", y_label="Score", name=name)

    return average_per_day


def getrolling_headline_pre(df: pd.DataFrame, filename: str) -> None:

    # pre covid headlines
    df = df[df['ERA'] == "pre"].copy()
    df["DATE"] = pd.to_datetime(df["DATE"])
    df = df.sort_values('DATE')
    # durchschnittsbildung über mean
    df1 = df[["DATE", "HEADLINE_SENTIMENT_SCORE"]]
    res = df1.groupby('DATE', as_index=False, sort=False)[
        'HEADLINE_SENTIMENT_SCORE'].mean()
    res['rolling_mean'] = res['HEADLINE_SENTIMENT_SCORE'].rolling(28).mean()
    avg_sentiment_list = [0]+res["rolling_mean"].tolist()[1:]

    # plotten avg sentiment
    title = "Durchschnittliches Sentiment vor Covid \n für Headlines"
    name = f"{filename}_sentiment_headline_precovid"
    format_plot(y=avg_sentiment_list, title=title,
                x_label="Zeit", y_label="Score", name=name)


def getrolling_headline_post(df: pd.DataFrame, filename: str) -> None:

    # post covid headlines
    df = df[df['ERA'] == "post"].copy()
    df["DATE"] = pd.to_datetime(df["DATE"])
    df = df.sort_values('DATE')
    # durchschnittsbildung über mean
    df1 = df[["DATE", "HEADLINE_SENTIMENT_SCORE"]]
    res = df1.groupby('DATE', as_index=False, sort=False)[
        'HEADLINE_SENTIMENT_SCORE'].mean()
    res['rolling_mean'] = res['HEADLINE_SENTIMENT_SCORE'].rolling(28).mean()
    avg_sentiment_list = [0]+res["rolling_mean"].tolist()[1:]

    # plotten avg sentiment
    title = "Durchschnittliches Sentiment nach Covid \n für Headlines"
    name = f"{filename}_sentiment_headline_postcovid"
    format_plot(y=avg_sentiment_list, title=title,
                x_label="Zeit", y_label="Score", name=name)


def getrolling_headline_overall(df: pd.DataFrame, filename: str) -> None:
    """
    positive, negative, neutral + total artikel mit deren avg. sentiment über den gesamten zeitraum
    für headlines
    """

    df1 = df[["DATE", "HEADLINE_SENTIMENT"]]

    # durchschnittsbildung
    out = (
        df1
        .groupby(['DATE', 'HEADLINE_SENTIMENT'])  # column als index
        .size()  # summe der gezählten rows
        .unstack(fill_value=0)
        .sort_index()
    )

    out = out.rolling('28D').sum()

    neutral_list = [0]+out["neutral"].tolist()[1:]
    positive_list = [0]+out["positive"].tolist()[1:]
    negative_list = [0]+out["negative"].tolist()[1:]

    # berechnung summe (total) artikel
    out["total"] = 0
    for index, row in out.iterrows():
        neutral = int(row["neutral"])
        positive = int(row["positive"])
        negative = int(row["negative"])
        total = neutral+positive+negative
        out.loc[index, 'total'] = total

    # total_list = [0]+out["total"].tolist()[1:]

    # date_list = ["week "+str(i//7) for i in range(len(neutral_list))]
    # plotten positive negative neutrale und artikel ings. für den gesamten zeitraum
    fig, ax = plt.subplots()
    fig.suptitle(
        'Kategorisierung von allen Headlines')
    fig.supxlabel('Zeit')
    fig.supylabel('Anzahl')
    for Y in [(neutral_list, "Neutral", "blue"), (positive_list, "Positiv", "green"), (negative_list, "Negativ", "red"),
              # (total_list, "Insgesamt", "orange")
              # total artikel nicht anzeigen
              ]:
        ax.plot(np.arange(len(neutral_list)), Y[0], color=Y[2], label=Y[1])
        ax.legend(loc="upper right")
    fig.autofmt_xdate()
    # plt.xticks(ticks=np.arange(len(neutral_list))
    #            [::28], labels=date_list[::28])
    plt.xticks(ticks=[])
    plt.savefig(filename+"_positive_negative_total_overtime_overall_headline" +
                ".png", bbox_inches='tight')
    # datum und sentimentscore auswählen
    df1 = df[["DATE", "HEADLINE_SENTIMENT_SCORE"]]
    res = df1.groupby('DATE', as_index=False, sort=False)[
        'HEADLINE_SENTIMENT_SCORE'].mean()
    res['rolling_mean'] = res['HEADLINE_SENTIMENT_SCORE'].rolling(28).mean()
    avg_sentiment_list = [0]+res["rolling_mean"].tolist()[1:]

    # plotten avg. sentiment
    title = "Durchschnittliches Sentiment für alle Headlines"
    name = f"{filename}_sentiment_aller_headlines"
    format_plot(y=avg_sentiment_list, title=title,
                x_label="Zeit", y_label="Score", name=name, x_line=True)


def getrolling_content_overall(df: pd.DataFrame, filename: str) -> None:

    df1 = df[["DATE", "CONTENT_SENTIMENT"]]

    out = (
        df1
        .groupby(['DATE', 'CONTENT_SENTIMENT'])  # columns als index
        .size()  # summe der gezählten rows
        .unstack(fill_value=0)
        .sort_index()
    )

    out = out.rolling('28D').sum()

    # erster wert als 0
    neutral_list = [0]+out["neutral"].tolist()[1:]
    positive_list = [0]+out["positive"].tolist()[1:]
    negative_list = [0]+out["negative"].tolist()[1:]

    # summe aller artikel
    out["total"] = 0
    for index, row in out.iterrows():
        neutral = int(row["neutral"])
        positive = int(row["positive"])
        negative = int(row["negative"])
        total = neutral+positive+negative
        out.loc[index, 'total'] = total

    # total_list = [0]+out["total"].tolist()[1:]

    # date_list = ["week"+str(i//7) for i in range(len(neutral_list))]

    # plot positive negative neutrale und alle artikel für den gesamten zeitraum
    fig, ax = plt.subplots()
    fig.suptitle(
        'Kategorisierung des Contents von allen Artikeln')
    fig.supxlabel('Zeit')
    fig.supylabel('Anzahl')
    for Y in [(neutral_list, "Neutral", "blue"), (positive_list, "Positiv", "green"), (negative_list, "Negativ", "red"),
              # (total_list, "Insgesamt", "orange")
              ]:
        # plt.xticks(ticks=np.arange(len(Y[0]))[::28], labels=date_list[::28])
        plt.xticks([])
        ax.plot(Y[0], color=Y[2], label=Y[1])
        ax.legend(loc="upper right")
    fig.autofmt_xdate()
    plt.savefig(filename+"_sentiment_content_gesamter_zeitraum" +
                ".png", bbox_inches='tight')
    # wähle datum und sentiment score
    df1 = df[["DATE", "CONTENT_SENTIMENT_SCORE"]]
    res = df1.groupby('DATE', as_index=False, sort=False)[
        'CONTENT_SENTIMENT_SCORE'].mean()
    res['rolling_mean'] = res['CONTENT_SENTIMENT_SCORE'].rolling(28).mean()
    avg_sentiment_list = [0]+res["rolling_mean"].tolist()[1:]
    avg_sentiment_list = avg_sentiment_list

    # plot avg. sentiment für gesamten Zeitraum
    title = "Durchschnittliches Sentiment für den gesamten Content"
    name = f"{filename}_avg_sentiments_overtime_overall_content"
    format_plot(y=avg_sentiment_list, title=title,
                x_label="Zeit", y_label="Score", name=name, x_line=True)


def get_sentiments(inputfiles, name=None):
    """
    :param inputfile: input filename(.xlsx)
    :param name: name of the file to save
    """
    # Zuweisung Name des Endfiles
    if name is None:
        starts = [file.split('/')[-1][:3] for file in inputfiles]
        name = f"{'_'.join([s for s in starts])}"

    analyzer = SentimentIntensityAnalyzer()
    df = pd.read_excel(inputfiles[0])
    for file in inputfiles[1:]:
        df = df.append(pd.read_excel(file), ignore_index=True)
    # print(len(df))
    # print(df)

    df.dropna(subset=['DATE'])

    # erstelle columns
    df['HEADLINE_SENTIMENT'] = ''
    df['CONTENT_SENTIMENT'] = ''
    df['HEADLINE_SENTIMENT_SCORE'] = 0  # speicherort für headlines sentiment
    df['CONTENT_SENTIMENT_SCORE'] = 0  # speicheort für content sentiment
    df['ERA'] = ''  # pre-covid oder post-covid

    # Columns als String umwandeln mit typecast
    df['HEADLINE'] = df['HEADLINE'].astype(str)
    df['CONYTENT'] = df['CONTENT'].astype(str)
    df['HEADLINE'] = df['HEADLINE'].fillna('')
    df['CONTENT'] = df['CONTENT'].fillna('')
    df['DATE'] = pd.to_datetime(df['DATE'])
    df['YEAR'] = df['DATE'].dt.year

    for index, row in tqdm.tqdm(df.iterrows(), total=df.shape[0]):

        # überprüfung ob pre oder post covid
        if int(row["YEAR"]) < 2020:
            era = "pre"
        else:
            era = "post"
        df.loc[index, 'ERA'] = era
        # ermittlung sentiment für headline
        vs = analyzer.polarity_scores(row["HEADLINE"])
        score = vs['compound']
        df.loc[index, 'HEADLINE_SENTIMENT_SCORE'] = score
        if score >= 0.05:
            score = 'positive'
            df.loc[index, 'HEADLINE_SENTIMENT'] = score

        elif score <= -0.05:
            score = 'negative'
            df.loc[index, 'HEADLINE_SENTIMENT'] = score

        else:
            score = 'neutral'
            df.loc[index, 'HEADLINE_SENTIMENT'] = score

        # ermittlung sentiment für content
        vs = analyzer.polarity_scores(row["CONTENT"])
        score = vs['compound']
        df.loc[index, 'CONTENT_SENTIMENT_SCORE'] = score
        if score >= 0.05:
            score = 'positive'
            df.loc[index, 'CONTENT_SENTIMENT'] = score

        elif score <= -0.05:
            score = 'negative'
            df.loc[index, 'CONTENT_SENTIMENT'] = score

        else:
            score = 'neutral'
            df.loc[index, 'CONTENT_SENTIMENT'] = score
    # speichern der sentiment files
    df.to_excel(str(name)+"_sentiment.xlsx", index=False)
    df["DATE"] = pd.to_datetime(df["DATE"])
    df = df.sort_values('DATE')
    # topics = df['TOPICS'].unique()

    getrolling_headline_overall(df, name)
    getrolling_content_overall(df, name)
    avg_per_day_pre = int(getrolling_content_pre(df, name))
    avg_per_day_post = int(getrolling_content_post(df, name))
    getrolling_headline_pre(df, name)
    getrolling_headline_post(df, name)

    # plots
    plt.clf()
    data = {'Vor Corona': avg_per_day_pre, 'Nach Corona': avg_per_day_post, }
    keys = list(data.keys())
    values = list(data.values())

    _, ax = plt.subplots()
    # label für die Achsen hinzufügen
    ax.set_ylabel('Artikel pro Tag')
    ax.set_title('Artikel pro Tag vor \n und nach Corona')
    ax.bar(keys, values, color='maroon', width=0.4)

    # speichern der grafik
    plt.savefig(
        str(name)+"_avg_artikel_pro_tag_vor_und_nach_corona.png", bbox_inches='tight')


if __name__ == "__main__":
    get_sentiments(
        ["/Users/Aleks/Desktop/Masterarbeit/Python Sentiment und Wordcloud/GerVADER/FAZ_SCRAPING_BEREINIGT_12.07.2022.xlsx"])
