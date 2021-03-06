package it.fdev.unisaconnect;

import it.fdev.scraper.MenuMensaScraper;
import it.fdev.unisaconnect.data.MenuMensa;
import it.fdev.unisaconnect.data.MenuMensa.PiattoMensa;
import it.fdev.unisaconnect.data.SharedPrefDataManager;
import it.fdev.utils.ExpandablePanel;
import it.fdev.utils.MyDateUtils;
import it.fdev.utils.MySimpleFragment;
import it.fdev.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FragmentMensa extends MySimpleFragment {
	
	private SharedPrefDataManager mDataManager;
	private MenuMensaScraper mensaScraper;
	private boolean alreadyStarted = false;
	private MenuMensa menu;
	
	private TextView lastUpdateTimeView;
	private View lastUpdateIconView;
	private View menuContainerView;
	private TextView menuNDView;

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View mainView = (View) inflater.inflate(R.layout.fragment_mensa, container, false);
		return mainView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		lastUpdateTimeView = (TextView) view.findViewById(R.id.last_update_time);
		lastUpdateIconView =  view.findViewById(R.id.last_update_icon);
		menuContainerView = view.findViewById(R.id.menu_list_container);
		menuNDView = (TextView) view.findViewById(R.id.menu_non_disponibile);
		mDataManager = new SharedPrefDataManager(mActivity);
		
		mActivity.setLoadingVisible(true, true);
		
		menu = mDataManager.getMenuMensa();
		if (menu != null) {
			Log.d(Utils.TAG, "Menu salvato!");
		}
		getMenu(false);
	}

	
	@Override
	public Set<Integer> getActionsToShow() {
		Set<Integer> actionsToShow = new HashSet<Integer>();
		actionsToShow.add(R.id.action_refresh_button);
		if (!alreadyStarted) {
			actionsToShow.add(R.id.action_loading_animation);
		}
		return actionsToShow;
	}

	@Override
	public void actionRefresh() {
		getMenu(true);
	}

	public void getMenu(boolean force) {
		if (!isAdded()) {
			return;
		}

		mActivity.setLoadingVisible(true, true);
		
		if (force) {
			menu = null;
			mDataManager.setMenuMensa(null);
		}
		
		if (!force && menu != null) {
			Calendar lastUpdateTime = new GregorianCalendar();
			lastUpdateTime.setTime(menu.getFetchTime());
			Calendar now = new GregorianCalendar();
			now.setTime(new Date());
			// Se il menu è di oggi
			if (lastUpdateTime.get(GregorianCalendar.DAY_OF_MONTH) == now.get(GregorianCalendar.DAY_OF_MONTH) &&
			   lastUpdateTime.get(GregorianCalendar.MONTH) == now.get(GregorianCalendar.MONTH) &&
			   lastUpdateTime.get(GregorianCalendar.YEAR) == now.get(GregorianCalendar.YEAR)) {
				alreadyStarted = true;
				mostraMenu(null);
				return;
			} else {
				// Il menu è vecchio, lo cancello e continuo
				menu = null;
				mDataManager.setMenuMensa(null);
			}
		}
		
		if (!Utils.hasConnection(mActivity)) {
			Utils.goToInternetError(mActivity, this);
			return;
		}
		
		if (force || !alreadyStarted) {
			alreadyStarted = true;
			if (mensaScraper != null && mensaScraper.isRunning) {
				return;
			}
			menu = null;
			mDataManager.setMenuMensa(null);
			mensaScraper = new MenuMensaScraper();
			mensaScraper.setCallerMenuMensaFragment(this);
			mensaScraper.execute(mActivity);
			return;
		} else {
			mostraMenu(null);
		}
	}

	public void mostraMenu(MenuMensa menu) {
		if (!isAdded()) {
			return;			
		}
		if (menuContainerView == null || menuNDView == null) { // Dai report di crash sembra succedere a volte, non ho idea del perchè
			mActivity.setDrawerOpen(true);							   // Quindi mostro lo slidingmenu per apparare
			mActivity.setLoadingVisible(false, false);
			return;
		}
		if (menu == null && this.menu == null) {
			menuContainerView.setVisibility(View.GONE);
			menuNDView.setVisibility(View.VISIBLE);
			mActivity.setLoadingVisible(false, false);
			return;
		} else {
			menuContainerView.setVisibility(View.VISIBLE);
			menuNDView.setVisibility(View.GONE);
		}
		if (menu != null) {
			this.menu = menu;
		}
		
		String updateText;
		try {
			SimpleDateFormat inputFormatterTime = new SimpleDateFormat("dd/MM/yy", Locale.ITALY);
			Date date = inputFormatterTime.parse(this.menu.getDate());
//			long millis = Long.parseLong(this.menu.getDateMillis());
			updateText = MyDateUtils.getLastUpdateString(mActivity, date.getTime(), true);
		} catch (Exception e) {
			updateText = this.menu.getDate();
		}
		if (updateText != null && !updateText.isEmpty()) {
			lastUpdateTimeView.setText(updateText);
			lastUpdateTimeView.setVisibility(View.VISIBLE);
			lastUpdateIconView.setVisibility(View.VISIBLE);
		} else {
			lastUpdateTimeView.setVisibility(View.GONE);
			lastUpdateIconView.setVisibility(View.GONE);
		}
		
		LayoutInflater layoutInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout menuListView = (LinearLayout) mActivity.findViewById(R.id.menu_list);
		menuListView.removeAllViews();

		ArrayList<PiattoMensa> firstCourses = this.menu.getFirstCourses();
		ArrayList<PiattoMensa> secondCourses = this.menu.getSecondCourses();
		ArrayList<PiattoMensa> sideCourses = this.menu.getSideCourses();
		ArrayList<PiattoMensa> fruitCourses = this.menu.getFruitCourses();
		ArrayList<PiattoMensa> takeAwayCourses = this.menu.getTakeAwayBasketCourses();
		ArrayList<PiattoMensa> otherCourses = this.menu.getOtherCourses();

		if (firstCourses != null && firstCourses.size() > 0) {
			LinearLayout courseView = inflateCourse(firstCourses, getString(R.string.mensa_primi), layoutInflater);
			menuListView.addView(courseView);
		}

		if (secondCourses != null && secondCourses.size() > 0) {
			LinearLayout courseView = inflateCourse(secondCourses, getString(R.string.mensa_secondi), layoutInflater);
			menuListView.addView(courseView);
		}

		if (sideCourses != null && sideCourses.size() > 0) {
			LinearLayout courseView = inflateCourse(sideCourses, getString(R.string.mensa_contorni), layoutInflater);
			menuListView.addView(courseView);
		}

		if (otherCourses != null) {
			LinearLayout courseView = inflateCourse(otherCourses, getString(R.string.mensa_altro), layoutInflater);
			menuListView.addView(courseView);
		} else {
			if (fruitCourses != null && fruitCourses.size() > 0) {
				LinearLayout courseView = inflateCourse(fruitCourses, getString(R.string.mensa_frutta), layoutInflater);
				menuListView.addView(courseView);
			}
			if (takeAwayCourses != null && takeAwayCourses.size() > 0) {
				LinearLayout courseView = inflateCourse(takeAwayCourses, getString(R.string.mensa_centino), layoutInflater);
				menuListView.addView(courseView);
			}
		}
		
		if (menu != null) {
			// Il metodo è stato chiamato con il menu aggiornato da salvare
			Log.d(Utils.TAG, "Salvo il menu!");
			mDataManager.setMenuMensa(menu);
		}
		
		mActivity.setLoadingVisible(false, false);

	}
	
	public void mostraErrore(String errore) {
		menuContainerView.setVisibility(View.GONE);
		menuNDView.setText(errore);
		menuNDView.setVisibility(View.VISIBLE);
		mActivity.setLoadingVisible(false, false);
		return;
	}

	private LinearLayout inflateCourse(ArrayList<PiattoMensa> courses, String name, LayoutInflater layoutInflater) {
		LinearLayout courseView = (LinearLayout) layoutInflater.inflate(R.layout.mensa_course, null);
		
		TextView labelView = (TextView) courseView.findViewById(R.id.course_label);
		labelView.setText(name);

		for (PiattoMensa cCourse : courses) {
			String nome = cCourse.getNomePiatto();
			String ingredientiIT = cCourse.getIngredientiIt();
			String ingredientiEN = cCourse.getIngradientiEn();
			if (nome != null) {
				LinearLayout detailsView = (LinearLayout) layoutInflater.inflate(R.layout.mensa_course_details, null);
				ExpandablePanel expPanel = (ExpandablePanel) detailsView.findViewById(R.id.exp_panel);
				TextView nameView = (TextView) detailsView.findViewById(R.id.course_name);
				TextView ingredientsITView = (TextView) detailsView.findViewById(R.id.course_ingredients_it);
				TextView ingredientsENView = (TextView) detailsView.findViewById(R.id.course_ingredients_en);
				nameView.setText(nome);
				if (ingredientiIT == null || ingredientiIT.isEmpty()) {
					ingredientsITView.setVisibility(TextView.GONE);
				} else {
					ingredientsITView.setText(ingredientiIT);
				}
				if (ingredientiEN == null || ingredientiEN.isEmpty()) {
					ingredientsENView.setVisibility(TextView.GONE);
				} else {
					ingredientsENView.setText(ingredientiEN);
				}
				if ((ingredientiIT == null || ingredientiIT.isEmpty()) && (ingredientiEN == null || ingredientiEN.isEmpty())) {
					expPanel.hideHandle();
				}
				courseView.addView(detailsView);
			}
		}
		return courseView;
	}
	
	@Override
	public void onStop() {
		if (mensaScraper != null && mensaScraper.isRunning) {
			mensaScraper.cancel(true);
		}
		super.onStop();
	}
	
	@Override
	public int getTitleResId() {
		return R.string.menu_mensa;
	}
}
