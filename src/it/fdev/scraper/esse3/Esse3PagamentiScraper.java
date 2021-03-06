package it.fdev.scraper.esse3;

import it.fdev.unisaconnect.data.Pagamenti;
import it.fdev.unisaconnect.data.Pagamenti.Pagamento;
import it.fdev.unisaconnect.data.SharedPrefDataManager;
import it.fdev.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;

import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.R.integer;
import android.content.Context;
import android.util.Log;

public class Esse3PagamentiScraper extends Esse3BasicScraper {
	public final String pagamentiURL = "https://esse3web.unisa.it/unisa/auth/studente/Tasse/ListaFatture.do";

	public Esse3PagamentiScraper(Context context, SharedPrefDataManager dataManager, String base64login, String broadcastID) {
		super(context, dataManager, base64login, broadcastID);
	}

	@Override
	public LoadStates startScraper() {
		try {
			Pagamenti pagamenti = scraperStepPagamenti();
			if (pagamenti != null) {
				mDataManager.setPagamenti(pagamenti);
				// dataManager.saveData();
			}
			return LoadStates.FINISHED;
			// return LoadStates.NO_DATA;
		} catch (HttpStatusException e) {
			Log.d(Utils.TAG, "Damn");
			Log.w(Utils.TAG, "ERROR ", e);
			int code = e.getStatusCode();
			if (code == 401)
				return LoadStates.WRONG_DATA;
			else
				return LoadStates.UNKNOWN_PROBLEM;
		} catch (Exception e) {
			Log.d(Utils.TAG, "Damn1");
			Log.w(Utils.TAG, "ERROR ", e);
			return LoadStates.UNKNOWN_PROBLEM;
		}
	}

	private Pagamenti scraperStepPagamenti() throws HttpStatusException, IOException, InterruptedException {
		Document document = scraperGetUrl(pagamentiURL);

		if (document == null) {
			return null;
		}

		ArrayList<Pagamento> pagamentiList = new ArrayList<Pagamento>();

		Elements pnpElement = document.getElementsMatchingOwnText("Pagamento non pervenuto");
		
		if (pnpElement.size() < 1) {
			return null;
		}

		Element row = pnpElement.first().parent();
		while (row != null && !"tr".equalsIgnoreCase(row.tagName())) {
			row = row.parent();
		}
		
		if (row == null) {
			return null;
		}
		
		Pagamento pagamento = null;
		row = row.nextElementSibling();
		while (row != null && ! row.classNames().contains("tplTitolo")) {
			Elements cells = row.getElementsByTag("td");
			if (cells.size() == 7) {
				String pagamentoScadenza = cells.get(4).text();
				String pagamentoCausale =  cells.get(3).text();
				String pagamentoImporto = cells.get(5).text();
				pagamento = new Pagamento(pagamentoCausale.trim(), pagamentoImporto.trim(), pagamentoScadenza.trim());
				pagamentiList.add(pagamento);
			} else if (cells.size() == 1 && pagamento != null) {
				String pagamentoCausale =  cells.get(0).text();
				pagamento.addCausale(pagamentoCausale.trim());
			} 
			row = row.nextElementSibling();
		}

		return new Pagamenti(pagamentiList);
	}

}
