package com.scrapers.website;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.scraper.model.Article;
import com.scraper.model.Publisher;

public class Sueddeutsche {
	final static Logger logger = Logger.getLogger(Sueddeutsche.class);
	public final static String[] CSV_HEADER = { "PUBLISHER", "DATE", "AUTHOR", "TOPICS", "HEADLINE", "TEASER",
			"CONTENT", "URL" };
	private static final Publisher THIS_PUBLISHER = Publisher.SUEDDEUTSCHE_DE;
	public final static String BROWSER_USER_AGENT_HEADER = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36";
	private static final SimpleDateFormat dateCSVExportFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	
	private WebClient browser;

	private String outputFilePath;
	public String outputFolderPath;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Sueddeutsche application = new Sueddeutsche();
		
		application.outputFolderPath = System.getProperty("user.dir");
		application.start();
	}

	public void start() {
		try {

			SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
			logger.info("start " + THIS_PUBLISHER + " extraction : " + formater.format(new Date()));

			this.outputFilePath = this.outputFolderPath + System.getProperty("file.separator") + THIS_PUBLISHER.name()
					+ "_" + formater.format(new Date()) + ".csv";

			FileWriter out = new FileWriter(this.outputFilePath, true);
			CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(CSV_HEADER));
			printer.flush();
			printer.close();

			java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
			java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);

			logger.info("initialize virtual browser ...");
			this.browser = new WebClient();
			this.browser.getOptions().setCssEnabled(false);
			this.browser.getOptions().setJavaScriptEnabled(false);
			this.browser.getOptions().setThrowExceptionOnFailingStatusCode(false);
			this.browser.getOptions().setThrowExceptionOnScriptError(false);
			this.browser.getOptions().setTimeout(120 * 1000);
			this.browser.getOptions().setRedirectEnabled(true);
			this.browser.getOptions().setUseInsecureSSL(true);
			this.browser.addRequestHeader("user-agent", BROWSER_USER_AGENT_HEADER);

			
			
			/* zunächst auf die hauptseite des archives gehen, mit XPATH selector werden 'topics' gesucht,
			 * für jedes topic werden die verfügbaren jahre überprüft
			 * */

			HtmlPage mainPage = this.browser.getPage("https://www.sueddeutsche.de/archiv");
			List<DomElement> listAElement = mainPage.getByXPath("//div[@class='department-overview-title']/a");
			for (DomElement aElement : listAElement) {
				logger.info(" department : " + aElement.getTextContent().trim());
				extractTopic("https://www.sueddeutsche.de" + aElement.getAttribute("href"));
			}

		} catch (Exception ex) {
			logger.error("method start error : ", ex);
		}
	}

	public void extractTopic(String topicURL) {
		try {

			HtmlPage topicPage = this.browser.getPage(topicURL);
			List<DomElement> listYearAElement = topicPage
					.getByXPath("//div[@class='department-overview-filter'][1]//a");
			for (DomElement yearAElement : listYearAElement) {
				logger.info("     year : " + yearAElement.getTextContent().trim());
				String link = "https://www.sueddeutsche.de" + yearAElement.getAttribute("href");
				browseArticles(link);
			}

		} catch (Exception ex) {
			logger.error("method extractTopic error : ", ex);
		}
	}

	public void browseArticles(String listingsURL) {
		try {

			int pageIndex = 0;
			boolean keepGoing = true;
			while (keepGoing) {
				try {

					pageIndex += 1;
					keepGoing = false;

					HtmlPage articlesPage = this.browser.getPage(listingsURL + "/page/" + pageIndex);
					List<DomElement> listArticleAElement = articlesPage.getByXPath(
							"//div[@id='entrylist-container']//div[@class='entrylist__entry']//a[@class='entrylist__link']");

					if (listArticleAElement.size() > 0) {
						logger.info("        page : " + pageIndex);
						keepGoing = true;
						for (DomElement articleAElement : listArticleAElement)
							extractArticle(articleAElement.getAttribute("href"));
					}

				} catch (Exception e) {
					logger.error("issue : ", e);
				}
			}

			logger.info("completed");

		} catch (Exception ex) {
			logger.error("method browseArticles error : ", ex);
		}
	}

	public void extractArticle(String articleURL) {
		try {

			Article article = new Article(THIS_PUBLISHER, articleURL);

			logger.info("              " + article.getUrl());

			HtmlPage articlePage = this.browser.getPage(articleURL);
			// Thread.sleep(randomInRange(500, 700));

			DomElement titleElement = articlePage.getFirstByXPath("//meta[@property='og:title']");
			if (titleElement != null)
				article.setHeadline(titleElement.getAttribute("content"));

			DomElement authorElement = articlePage.getFirstByXPath("//meta[@name='author']");
			if (authorElement != null)
				article.setAuthor(authorElement.getAttribute("content"));

			// topic ist immer das zweite item in der liste BreadcrumbList
			DomElement topicElement = articlePage
					.getFirstByXPath("//ol[@typeof='BreadcrumbList']//li[@property='itemListElement'][2]/a/span");
			if (topicElement != null)
				article.setTopics(StringUtils.normalizeSpace(topicElement.getTextContent()).trim());

			
			// teaser und datum sind beide als HTML attribute im DIV
			
			DomElement divElement = articlePage.getFirstByXPath("//div[@id='taboola-feed-below-article']");
			if (divElement != null) {
				article.setTeaser(StringUtils.normalizeSpace(divElement.getAttribute("data-teaser")).trim());
				String datetime = divElement.getAttribute("data-publishdate");
				article.setDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(datetime));
			}

			DomElement articleBodyElement = articlePage.getFirstByXPath("//div[@itemprop='articleBody']");
			if (articleBodyElement != null)
				article.setContent(StringUtils.normalizeSpace(articleBodyElement.asText()).trim());

			writeToCSV(article);

		} catch (Exception ex) {
			logger.error("method extractArticle error : ", ex);
		}
	}

	public void writeToCSV(Article article) throws IOException {
		OutputStream outputStream = new FileOutputStream(this.outputFilePath, true);
		Writer outputStreamWriter = new OutputStreamWriter(outputStream, "UTF-8");

		CSVPrinter printer = new CSVPrinter(outputStreamWriter, CSVFormat.DEFAULT);
		printer.printRecord(article.getPublisher().toString(), dateCSVExportFormat.format(article.getDate()),
				article.getAuthor(), article.getTopics(), article.getHeadline(), article.getTeaser(),
				article.getContent(), article.getUrl());

		printer.flush();
		printer.close();
	}

	private int randomInRange(int min, int max) {
		return new Random().nextInt((max - min) + 1) + min;
	}

}
