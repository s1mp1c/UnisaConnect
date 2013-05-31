package it.fdev.unisaconnect;

import it.fdev.scrapers.WeatherScraper;
import it.fdev.unisaconnect.data.WeatherData;
import it.fdev.utils.MySimpleFragment;
import it.fdev.utils.Utils;

import java.util.Locale;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.slidingmenu.lib.SlidingMenu;
import com.viewpagerindicator.TitlePageIndicator;

public class WeatherFragment extends MySimpleFragment {

	private boolean alreadyStarted = false;
	private WeatherScraper meteoScraper;
	private WeatherData meteo;

	private RelativeLayout weatherActualContainerView;
	private GridView weatherForecastGridview;
	private TextView meteoNDView;
	private ViewPager pager;
	private TitlePageIndicator indicator;

	private TextView lastUpdateTimeView;
	private ImageView iconView;
	private TextView tempView;
	private TextView descriptionView;
	private TextView humidityView;
	private TextView windView;
	private TextView windDirView;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View mainView = (View) inflater.inflate(R.layout.weather, container, false);
		return mainView;
	}

	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		meteoNDView = (TextView) activity.findViewById(R.id.meteo_non_disponibile);
		pager = (ViewPager) view.findViewById(R.id.meteo_pager);
		indicator = (TitlePageIndicator) view.findViewById(R.id.meteo_indicator);

		lastUpdateTimeView = (TextView) view.findViewById(R.id.last_update_time);
		iconView = (ImageView) view.findViewById(R.id.weather_icon);
		tempView = (TextView) view.findViewById(R.id.weather_temp);
		descriptionView = (TextView) view.findViewById(R.id.weather_description);
		humidityView = (TextView) view.findViewById(R.id.weather_humidity);
		windView = (TextView) view.findViewById(R.id.weather_wind);
		windDirView = (TextView) view.findViewById(R.id.weather_wind_dir);

		weatherActualContainerView = (RelativeLayout) view.findViewById(R.id.weather_actual_container);
		weatherForecastGridview = (GridView) view.findViewById(R.id.weather_forecast_gridview);

		getMeteo(false);
	}

	class CurrentWeatherAdapter extends FragmentPagerAdapter {
		private WeatherActualConditionFragment[] fragmentsList;

		public CurrentWeatherAdapter(FragmentManager fm) {
			super(fm);
			fragmentsList = new WeatherActualConditionFragment[getCount()];
		}

		@Override
		public WeatherActualConditionFragment getItem(int position) {
			WeatherActualConditionFragment cFragment;
			if (fragmentsList[position] == null) {
				cFragment = new WeatherActualConditionFragment();
				cFragment.setCondition(meteo.getActualCondition(position));
				cFragment.setViews(lastUpdateTimeView, iconView, tempView, descriptionView, humidityView, windView, windDirView);
				fragmentsList[position] = cFragment;
			} else {
				cFragment = fragmentsList[position];
			}
			return cFragment;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return meteo.getActualCondition(position).getStationName().toUpperCase(Locale.ITALY);
		}

		@Override
		public int getCount() {
			return meteo.getActualConditionList().size();
		}
	}

	@Override
	public void setVisibleActions() {
		activity.setActionRefreshVisible(true);
	}

	@Override
	public void actionRefresh() {
		getMeteo(true);
	}

	@Override
	public void onResume() {
//		mostraMeteo(null);
		activity.getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
		super.onResume();
	}

	@Override
	public void onPause() {
		activity.getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		super.onPause();
	}

	public void getMeteo(boolean force) {
		if (!isAdded()) {
			return;
		}
		if (!Utils.hasConnection(activity)) {
			Utils.goToInternetError(activity, this);
			return;
		}
		if (force || !alreadyStarted) {
			alreadyStarted = true;
			if (meteoScraper != null && meteoScraper.isRunning) {
				return;
			}
			Utils.createDialog(activity, getString(R.string.caricamento), false);
			meteoScraper = new WeatherScraper(force);
			meteoScraper.setCallerMeteoFragment(this);
			meteoScraper.execute(activity);
		} else {
			mostraMeteo(meteo);
		}
	}

	public void mostraMeteo(WeatherData weatherData) {
		if (!isAdded()) {
			return;			
		}
		if (weatherData == null && this.meteo == null) {
			weatherActualContainerView.setVisibility(View.GONE);
			weatherForecastGridview.setVisibility(View.GONE);
			meteoNDView.setVisibility(View.VISIBLE);
			Log.d(Utils.TAG, "Meteo is null");
			activity.getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
			return;
		}
		weatherActualContainerView.setVisibility(View.VISIBLE);
		weatherForecastGridview.setVisibility(View.VISIBLE);
		meteoNDView.setVisibility(View.GONE);
		activity.getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
		
		if (weatherData != null) {
			this.meteo = weatherData;
		}
		
		final CurrentWeatherAdapter currentAdapter = new CurrentWeatherAdapter(activity.getSupportFragmentManager());
		pager.setAdapter(currentAdapter);
		indicator.setViewPager(pager);
		indicator.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageSelected(int arg0) {
				currentAdapter.getItem(arg0).showCondition();
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});
		indicator.setCurrentItem(1);
		weatherForecastGridview.setAdapter(new WeatherForecastAdapter(activity, this.meteo.getDailyForecastList()));
	}

}
