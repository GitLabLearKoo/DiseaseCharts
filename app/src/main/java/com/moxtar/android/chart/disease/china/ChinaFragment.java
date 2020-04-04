package com.moxtar.android.chart.disease.china;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.moxtar.android.chart.disease.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 简介：与MainActivity协作运行，用于控制国内疫情页面的Fragment类。
 */
public class ChinaFragment extends Fragment {
    // 线程池和首次刷新标志
    private ExecutorService mRefreshExecutor;
    private boolean isInitialized;
    // 一堆控件
    private LineChart mTrendChart;
    private PieChart mDistributionChart;
    private BarChart mProvinceChart;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private TextView mTVIntroExistingConfirmed;
    private TextView mTVIntroNoSymptom;
    private TextView mTVIntroInput;
    private TextView mTVIntroConfirmed;
    private TextView mTVIntroDead;
    private TextView mTVIntroCured;
    private TextView mTVIntroDate;

    private RadioButton mRadioTotalCured;
    private RadioButton mRadioTotalDeath;
    private RadioButton mRadioTotalConfirmed;
    private RadioButton mRadioExistingConfirmed;

    private Switch mSwitchConfirmed;
    private Switch mSwitchDead;
    private Switch mSwitchCured;
    private Switch mSwitchSuspected;
    private Switch mSwitchConfirmedExisting;
    // Subject和一堆Observer
    private ChinaDataSubject mChinaDataSubject;
    private DistributionChartDrawer mDistributionChartDrawer;
    private IntroductionDrawer mIntroductionDrawer;
    private TrendChartDrawer mTrendChartDrawer;
    private ProvinceChartDrawer mProvinceChartDrawer;

    /**
     * 简介：不准自主调用的构造方法，方法体只能为空。
     *       Fragment生命周期的缘故，横竖屏切换时Fragment会重新构建。这个方法不能保存参数
     */
    public ChinaFragment() {}

    /**
     * 简介：生成新对象的方法，可以添加相关参数，防止横竖屏切换时丢失参数。
     * @return 返回对象实例
     */
    public static ChinaFragment newInstance() {
        return new ChinaFragment();
    }

    /**
     * 简介：这个方法中可以获得布局及其控件，完成各种初始化工作。
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_china, container, false);
        // 单例线程调度器，串行执行更新线程
        mRefreshExecutor = Executors.newSingleThreadExecutor();
        // 初始化标志，只有第一次加载时自动刷新一次
        isInitialized = false;
        // 创建Subject
        mChinaDataSubject = new ChinaDataSubject();
        // 下拉刷新
        mSwipeRefreshLayout = view.findViewById(R.id.china_refresh);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mRefreshExecutor.execute(new RefreshRunnable());
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
        {   // *Introduction* //
            mTVIntroExistingConfirmed = view.findViewById(R.id.tv_intro_existing_confirmed);
            mTVIntroNoSymptom = view.findViewById(R.id.tv_intro_no_symptom);
            mTVIntroInput = view.findViewById(R.id.tv_intro_input);
            mTVIntroConfirmed = view.findViewById(R.id.tv_intro_confirmed);
            mTVIntroDead = view.findViewById(R.id.tv_intro_dead);
            mTVIntroCured = view.findViewById(R.id.tv_intro_cured);
            mTVIntroDate = view.findViewById(R.id.tv_intro_date);
            // 包装TextView
            Map<String, TextView> textViewMap = new HashMap<>();
            textViewMap.put("tvIntroExistingConfirmed", mTVIntroExistingConfirmed);
            textViewMap.put("tvIntroNoSymptom", mTVIntroNoSymptom);
            textViewMap.put("tvIntroInput", mTVIntroInput);
            textViewMap.put("tvIntroConfirmed", mTVIntroConfirmed);
            textViewMap.put("tvIntroDead", mTVIntroDead);
            textViewMap.put("tvIntroCured", mTVIntroCured);
            textViewMap.put("tvIntroDate", mTVIntroDate);
            // 创建Drawer
            mIntroductionDrawer = new IntroductionDrawer(textViewMap, getActivity());
            mChinaDataSubject.addObserver(mIntroductionDrawer);
        }
        {   // *DistributionChart* //
            mDistributionChart = view.findViewById(R.id.distribution_chart);
            mSwitchConfirmedExisting = view.findViewById(R.id.switch_confirmed_existing);
            // 创建Drawer
            mDistributionChartDrawer = new DistributionChartDrawer(mDistributionChart, getActivity());
            mChinaDataSubject.addObserver(mDistributionChartDrawer);
            // 设置控件
            mSwitchConfirmedExisting.setChecked(false);
            mSwitchConfirmedExisting.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mDistributionChartDrawer.setTotalConfirmedEnabled(isChecked);
                }
            });
        }
        {   // *ProvinceChart* //
            mProvinceChart = view.findViewById(R.id.province_chart);
            mRadioTotalCured = view.findViewById(R.id.radio_total_cured);
            mRadioTotalDeath = view.findViewById(R.id.radio_total_dead);
            mRadioTotalConfirmed = view.findViewById(R.id.radio_total_confirmed);
            mRadioExistingConfirmed = view.findViewById(R.id.radio_existing_confirmed);
            // 创建Drawer
            mProvinceChartDrawer = new ProvinceChartDrawer(mProvinceChart, getActivity());
            mChinaDataSubject.addObserver(mProvinceChartDrawer);
            // 设置控件
            mRadioTotalCured.setChecked(true);
            mRadioTotalCured.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        mProvinceChartDrawer.loadTotalCured();
                    }
                }
            });
            mRadioTotalDeath.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        mProvinceChartDrawer.loadTotalDead();
                    }
                }
            });
            mRadioTotalConfirmed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        mProvinceChartDrawer.loadTotalConfirmed();
                    }
                }
            });
            mRadioExistingConfirmed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        mProvinceChartDrawer.loadExistingConfirmed();
                    }
                }
            });
        }
        {   // *TrendChart* //
            mTrendChart = view.findViewById(R.id.trend_chart);
            mSwitchConfirmed = view.findViewById(R.id.switch_confirmed);
            mSwitchDead = view.findViewById(R.id.switch_dead);
            mSwitchCured = view.findViewById(R.id.switch_cured);
            mSwitchSuspected = view.findViewById(R.id.switch_suspected);
            // 创建Drawer
            mTrendChartDrawer = new TrendChartDrawer(mTrendChart, getActivity());
            mChinaDataSubject.addObserver(mTrendChartDrawer);
            // 设置控件
            mSwitchConfirmed.setChecked(true);
            mSwitchDead.setChecked(true);
            mSwitchCured.setChecked(true);
            mSwitchSuspected.setChecked(true);
            mSwitchConfirmed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mTrendChartDrawer.setConfirmedEnabled(isChecked);
                }
            });
            mSwitchDead.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mTrendChartDrawer.setDeadEnabled(isChecked);
                }
            });
            mSwitchCured.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mTrendChartDrawer.setCuredEnabled(isChecked);
                }
            });
            mSwitchSuspected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mTrendChartDrawer.setSuspectedEnabled(isChecked);
                }
            });
        }
        return view;
    }

    /**
     * 简介:可供线程运行的刷新类。
     */
    class RefreshRunnable implements Runnable {
        @Override
        public void run() {
            try {
                mChinaDataSubject.refreshData();
                printMessage(getResources().getString(R.string.msg_success_refresh));
            } catch (IOException e) {
                printMessage(getResources().getString(R.string.msg_err_request));
                e.printStackTrace();
            } catch (JSONException e) {
                printMessage(getResources().getString(R.string.msg_err_json));
                e.printStackTrace();
            }
        }
    }

    /**
     * 简介；运行于主线程上发送Toast的方法。为了防止泄露，不能使用Looper来发送Toast（线程不会消亡）。
     */
    private void printMessage(final String msg) {
        Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 简介：切换进入某个Fragment时就会调用的方法。首次刷新可以在这里调用。
     */
    @Override
    public void onResume() {
        super.onResume();
        // 首次刷新
        if (!isInitialized) {
            mRefreshExecutor.execute(new RefreshRunnable());
            isInitialized = true;
        }
    }
}
