package com.sbugert.rnadmob;

import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.views.view.ReactViewGroup;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.doubleclick.AppEventListener;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.doubleclick.PublisherAdView;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;

import com.amazon.device.ads.AdError;
import com.amazon.device.ads.AdRegistration;
import com.amazon.device.ads.DTBAdCallback;
import com.amazon.device.ads.DTBAdLoader;
import com.amazon.device.ads.DTBAdRequest;
import com.amazon.device.ads.DTBAdResponse;
import com.amazon.device.ads.DTBAdSize;
import com.amazon.device.ads.DTBAdUtil;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;;

public class RNPublisherBannerViewManager extends SimpleViewManager<ReactViewGroup> implements AppEventListener {

  public static final String REACT_CLASS = "RNAdMobDFP";

  public static final String PROP_BANNER_SIZE = "bannerSize";
  public static final String PROP_AD_UNIT_ID = "adUnitID";
  public static final String PROP_TEST_DEVICE_ID = "testDeviceID";
  public static final String PROP_SLOT_UUID = "slotUUID";
  public static final String PROP_CUSTOM_TARGETING = "customTargeting";

  private String testDeviceID = null;
  private String mSlotUUID = null;
  private ReadableMap mCustomTargeting = null;

  public enum Events {
    EVENT_SIZE_CHANGE("onSizeChange"), EVENT_RECEIVE_AD("onAdViewDidReceiveAd"), EVENT_ERROR(
        "onDidFailToReceiveAdWithError"), EVENT_WILL_PRESENT("onAdViewWillPresentScreen"), EVENT_WILL_DISMISS(
            "onAdViewWillDismissScreen"), EVENT_DID_DISMISS("onAdViewDidDismissScreen"), EVENT_WILL_LEAVE_APP(
                "onAdViewWillLeaveApplication"), EVENT_ADMOB_EVENT_RECEIVED("onAdmobDispatchAppEvent");

    private final String mName;

    Events(final String name) {
      mName = name;
    }

    @Override
    public String toString() {
      return mName;
    }
  }

  private ThemedReactContext mThemedReactContext;
  private RCTEventEmitter mEventEmitter;

  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Override
  public void onAppEvent(String name, String info) {
    String message = String.format("Received app event (%s, %s)", name, info);
    Log.d("PublisherAdBanner", message);
    WritableMap event = Arguments.createMap();
    event.putString(name, info);
    mEventEmitter.receiveEvent(viewID, Events.EVENT_ADMOB_EVENT_RECEIVED.toString(), event);
  }

  @Override
  protected ReactViewGroup createViewInstance(ThemedReactContext themedReactContext) {
    mThemedReactContext = themedReactContext;
    mEventEmitter = themedReactContext.getJSModule(RCTEventEmitter.class);
    ReactViewGroup view = new ReactViewGroup(themedReactContext);
    attachNewAdView(view);
    return view;
  }

  int viewID = -1;

  protected void attachNewAdView(final ReactViewGroup view) {
    final PublisherAdView adView = new PublisherAdView(mThemedReactContext);
    adView.setAppEventListener(this);
    // destroy old AdView if present
    PublisherAdView oldAdView = (PublisherAdView) view.getChildAt(0);
    view.removeAllViews();
    if (oldAdView != null)
      oldAdView.destroy();
    view.addView(adView);
    attachEvents(view);
  }

  protected void attachEvents(final ReactViewGroup view) {
    viewID = view.getId();
    final PublisherAdView adView = (PublisherAdView) view.getChildAt(0);
    adView.setAdListener(new AdListener() {
      @Override
      public void onAdLoaded() {
        int width = adView.getAdSize().getWidthInPixels(mThemedReactContext);
        int height = adView.getAdSize().getHeightInPixels(mThemedReactContext);
        int left = adView.getLeft();
        int top = adView.getTop();
        adView.measure(width, height);
        adView.layout(left, top, left + width, top + height);
        mEventEmitter.receiveEvent(view.getId(), Events.EVENT_RECEIVE_AD.toString(), null);
      }

      @Override
      public void onAdFailedToLoad(int errorCode) {
        WritableMap event = Arguments.createMap();
        switch (errorCode) {
        case PublisherAdRequest.ERROR_CODE_INTERNAL_ERROR:
          event.putString("error", "ERROR_CODE_INTERNAL_ERROR");
          break;
        case PublisherAdRequest.ERROR_CODE_INVALID_REQUEST:
          event.putString("error", "ERROR_CODE_INVALID_REQUEST");
          break;
        case PublisherAdRequest.ERROR_CODE_NETWORK_ERROR:
          event.putString("error", "ERROR_CODE_NETWORK_ERROR");
          break;
        case PublisherAdRequest.ERROR_CODE_NO_FILL:
          event.putString("error", "ERROR_CODE_NO_FILL");
          break;
        }

        mEventEmitter.receiveEvent(view.getId(), Events.EVENT_ERROR.toString(), event);
      }

      @Override
      public void onAdOpened() {
        mEventEmitter.receiveEvent(view.getId(), Events.EVENT_WILL_PRESENT.toString(), null);
      }

      @Override
      public void onAdClosed() {
        mEventEmitter.receiveEvent(view.getId(), Events.EVENT_WILL_DISMISS.toString(), null);
      }

      @Override
      public void onAdLeftApplication() {
        mEventEmitter.receiveEvent(view.getId(), Events.EVENT_WILL_LEAVE_APP.toString(), null);
      }
    });
  }

  @Override
  @Nullable
  public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
    MapBuilder.Builder<String, Object> builder = MapBuilder.builder();
    for (Events event : Events.values()) {
      builder.put(event.toString(), MapBuilder.of("registrationName", event.toString()));
    }
    return builder.build();
  }

  @ReactProp(name = PROP_BANNER_SIZE)
  public void setBannerSize(final ReactViewGroup view, final String sizeString) {
    AdSize adSize = getAdSizeFromString(sizeString);
    AdSize[] adSizes = new AdSize[1];
    adSizes[0] = adSize;

    // store old ad unit ID (even if not yet present and thus null)
    PublisherAdView oldAdView = (PublisherAdView) view.getChildAt(0);
    String adUnitId = oldAdView.getAdUnitId();

    attachNewAdView(view);
    PublisherAdView newAdView = (PublisherAdView) view.getChildAt(0);
    newAdView.setAdSizes(adSizes);
    newAdView.setAdUnitId(adUnitId);

    // send measurements to js to style the AdView in react
    int width;
    int height;
    WritableMap event = Arguments.createMap();
    if (adSize == AdSize.SMART_BANNER) {
      width = (int) PixelUtil.toDIPFromPixel(adSize.getWidthInPixels(mThemedReactContext));
      height = (int) PixelUtil.toDIPFromPixel(adSize.getHeightInPixels(mThemedReactContext));
    } else {
      width = adSize.getWidth();
      height = adSize.getHeight();
    }
    event.putDouble("width", width);
    event.putDouble("height", height);
    mEventEmitter.receiveEvent(view.getId(), Events.EVENT_SIZE_CHANGE.toString(), event);

    loadAd(newAdView);
  }

  @ReactProp(name = PROP_AD_UNIT_ID)
  public void setAdUnitID(final ReactViewGroup view, final String adUnitID) {
    // store old banner size (even if not yet present and thus null)
    PublisherAdView oldAdView = (PublisherAdView) view.getChildAt(0);
    AdSize[] adSizes = oldAdView.getAdSizes();

    attachNewAdView(view);
    PublisherAdView newAdView = (PublisherAdView) view.getChildAt(0);
    newAdView.setAdUnitId(adUnitID);
    newAdView.setAdSizes(adSizes);
    loadAd(newAdView);
  }

  @ReactProp(name = PROP_SLOT_UUID)
  public void setSlotUUID(final ReactViewGroup view, final String slotUUID) {
    mSlotUUID = slotUUID;
    PublisherAdView adView = (PublisherAdView) view.getChildAt(0);
    loadAd(adView);
  }

  @ReactProp(name = PROP_CUSTOM_TARGETING)
  public void setPropCustomTargeting(final ReactViewGroup view, final ReadableMap customTargeting) {
    mCustomTargeting = customTargeting;
    PublisherAdView adView = (PublisherAdView) view.getChildAt(0);
    loadAd(adView);
  }

  @ReactProp(name = PROP_TEST_DEVICE_ID)
  public void setPropTestDeviceID(final ReactViewGroup view, final String testDeviceID) {
    this.testDeviceID = testDeviceID;
  }

  private void loadAd(final PublisherAdView adView) {

    if (mSlotUUID != null && adView.getAdSizes() != null && adView.getAdUnitId() != null && mCustomTargeting != null) {
      final DTBAdRequest loader = new DTBAdRequest();

      loader.setSizes(new DTBAdSize(adView.getAdSize().getWidth(), adView.getAdSize().getHeight(), mSlotUUID));
      loader.loadAd(new DTBAdCallback() {
        @Override
        public void onFailure(AdError adError) {
          Log.e("AdError", "Oops banner ad load has failed: " + adError.getMessage());
          /**Please implement the logic to send ad request without our parameters if you want to
          show ads from other ad networks when Amazon ad request fails**/
          PublisherAdRequest.Builder adRequestBuilder = new PublisherAdRequest.Builder();

          if (mCustomTargeting.hasKey("pfSessionPiCount")) {
            int pfSessionPiCount = mCustomTargeting.getInt("pfSessionPiCount");
            adRequestBuilder.addCustomTargeting("pfSessionPiCount", Integer.toString(pfSessionPiCount));
          }
          if (mCustomTargeting.hasKey("pfTag")) {
            ReadableArray pfTag = mCustomTargeting.getArray("pfTag");
            List<String> pfTagList = new ArrayList<String>();
            int size = pfTag.size();
            for (int i = 0; i < size; i = i + 1) {
              pfTagList.add(pfTag.getString(i));
            }
            adRequestBuilder.addCustomTargeting("pfTag", pfTagList);
          }
          if (mCustomTargeting.hasKey("pfArticle")) {
            String pfArticle = mCustomTargeting.getString("pfArticle");
            adRequestBuilder.addCustomTargeting("pfArticle", pfArticle);
          }

          if (testDeviceID != null) {
            if (testDeviceID.equals("EMULATOR")) {
              adRequestBuilder = adRequestBuilder.addTestDevice(PublisherAdRequest.DEVICE_ID_EMULATOR);
            } else {
              adRequestBuilder = adRequestBuilder.addTestDevice(testDeviceID);
            }
          }
          PublisherAdRequest adRequest = adRequestBuilder.build();
          adView.loadAd(adRequest);
        }

        @Override
        public void onSuccess(DTBAdResponse dtbAdResponse) {
          PublisherAdRequest.Builder adRequestBuilder = DTBAdUtil.INSTANCE
              .createPublisherAdRequestBuilder(dtbAdResponse);

          if (mCustomTargeting.hasKey("pfSessionPiCount")) {
            int pfSessionPiCount = mCustomTargeting.getInt("pfSessionPiCount");
            adRequestBuilder.addCustomTargeting("pfSessionPiCount", Integer.toString(pfSessionPiCount));
          }
          if (mCustomTargeting.hasKey("pfTag")) {
            ReadableArray pfTag = mCustomTargeting.getArray("pfTag");
            List<String> pfTagList = new ArrayList<String>();
            int size = pfTag.size();
            for (int i = 0; i < size; i = i + 1) {
              pfTagList.add(pfTag.getString(i));
            }
            adRequestBuilder.addCustomTargeting("pfTag", pfTagList);
          }
          if (mCustomTargeting.hasKey("pfArticle")) {
            String pfArticle = mCustomTargeting.getString("pfArticle");
            adRequestBuilder.addCustomTargeting("pfArticle", pfArticle);
          }

          if (testDeviceID != null) {
            if (testDeviceID.equals("EMULATOR")) {
              adRequestBuilder = adRequestBuilder.addTestDevice(PublisherAdRequest.DEVICE_ID_EMULATOR);
            } else {
              adRequestBuilder = adRequestBuilder.addTestDevice(testDeviceID);
            }
          }
          final PublisherAdRequest adRequest = adRequestBuilder.build();
          adView.loadAd(adRequest);
        }
      });
    }
  }

  private AdSize getAdSizeFromString(String adSize) {
    switch (adSize) {
    case "banner":
      return AdSize.BANNER;
    case "largeBanner":
      return AdSize.LARGE_BANNER;
    case "mediumRectangle":
      return AdSize.MEDIUM_RECTANGLE;
    case "fullBanner":
      return AdSize.FULL_BANNER;
    case "leaderBoard":
      return AdSize.LEADERBOARD;
    case "smartBannerPortrait":
      return AdSize.SMART_BANNER;
    case "smartBannerLandscape":
      return AdSize.SMART_BANNER;
    case "smartBanner":
      return AdSize.SMART_BANNER;
    case "fliud":
      return AdSize.FLUID;
    case "skyscraper":
      return AdSize.WIDE_SKYSCRAPER;
    default:
      return AdSize.BANNER;
    }
  }
}
