package ac.robinson.kiosk;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.ToggleButton;

import com.google.android.material.snackbar.Snackbar;

import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

public class SurveyActivity extends AppCompatActivity {

	final String LOG_TAG = "SurveyKiosk";

	static int[] RESPONSE_GROUPS = new int[]{
			R.id.answer_group_1, R.id.answer_group_2, R.id.answer_group_3,
	};

	private String mSourceId;
	private SparseArray<Response> mResponses; // SparseArray rather than SparseBooleanArray so we can easily save state on rotate
	private View mRootView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_survey);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		if (savedInstanceState == null) {
			mResponses = new SparseArray<>();
			getSupportFragmentManager().beginTransaction().add(R.id.container, new SurveyResponseFragment()).commit();
		} else {
			mResponses = savedInstanceState.getSparseParcelableArray("mResponses");
		}

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		mSourceId = preferences.getString(getString(R.string.preference_key_source_id), null);

		if (TextUtils.isEmpty(mSourceId)) {
			mSourceId = UUID.randomUUID().toString();
			Editor editor = preferences.edit();
			editor.putString(getString(R.string.preference_key_source_id), mSourceId);
			editor.apply();
		}

		startKioskMode();
	}

	private void startKioskMode() {
		// pin this app so exiting is harder
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
			if (devicePolicyManager != null) {
				ComponentName deviceAdmin = new ComponentName(this, SurveyDeviceAdminReceiver.class);
				if (devicePolicyManager.isAdminActive(deviceAdmin)) {
					devicePolicyManager.setLockTaskPackages(deviceAdmin, new String[]{
							getPackageName()
					});
				}
			}
			startLockTask(); // pin screen regardless, even if we can't enter admin mode
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.survey, menu);

		new Handler().post(new Runnable() {
			@Override
			public void run() {
				View infoView = findViewById(R.id.action_info);
				if (infoView != null) {
					infoView.setOnLongClickListener(new View.OnLongClickListener() {
						@Override
						public boolean onLongClick(View clickedView) {
							SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(SurveyActivity.this);
							int logCount = preferences.getInt(getString(R.string.preference_key_response_count), 0);
							Snackbar.make(clickedView, getString(R.string.message_response_count, logCount),
									Snackbar.LENGTH_SHORT).show();
							return false;
						}
					});
				}
			}
		});
		return true;
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putSparseParcelableArray("mResponses", mResponses);
		super.onSaveInstanceState(savedInstanceState);
	}


	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		// we don't have any click actions for the hidden button
		return super.onOptionsItemSelected(item);
	}

	public void submitResponses(@SuppressWarnings("UnusedParameters") View ignored) {
		submitResponses();
	}

	public void submitResponses() {
		boolean validResult = true;
		for (int radioGroup : RESPONSE_GROUPS) {
			if (mResponses.indexOfKey(radioGroup) < 0) {
				validResult = false;
				break;
			}
		}

		if (mRootView == null) {
			mRootView = findViewById(R.id.container);
		}

		if (validResult) {

			// debugging only - submit or save responses here
			long logTime = System.currentTimeMillis();
			for (int i = 0; i < mResponses.size(); i++) {
				int key = mResponses.keyAt(i);
				Log.d(LOG_TAG, logTime + "," + mSourceId + "," + key + "," + mResponses.get(key).value);
			}

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
			int logCount = preferences.getInt(getString(R.string.preference_key_response_count), 0) + 1;
			Editor editor = preferences.edit();
			editor.putInt(getString(R.string.preference_key_response_count), logCount);
			editor.apply();

			resetState();
			Snackbar.make(mRootView, R.string.message_submitted, Snackbar.LENGTH_SHORT).show();

		} else {
			Snackbar.make(mRootView, R.string.message_invalid, Snackbar.LENGTH_SHORT).show();
		}
	}

	public void onToggle(View toggle) {
		RadioGroup parent = (RadioGroup) toggle.getParent();
		parent.clearCheck();
		parent.check(toggle.getId());
		mResponses.put(parent.getId(), new Response(toggle.getId() == R.id.btn_yes_1));
	}

	private void resetState() {
		for (int radioGroup : RESPONSE_GROUPS) {
			RadioGroup parent = findViewById(radioGroup);
			parent.clearCheck();
		}
		mResponses.clear();
	}

	private static class Response implements Parcelable {
		boolean value;

		public Response(boolean value) {
			this.value = value;
		}

		protected Response(Parcel in) {
			value = in.readByte() != 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeByte((byte) (value ? 1 : 0));
		}

		@Override
		public int describeContents() {
			return 0;
		}

		public static final Creator<Response> CREATOR = new Creator<Response>() {
			@Override
			public Response createFromParcel(Parcel in) {
				return new Response(in);
			}

			@Override
			public Response[] newArray(int size) {
				return new Response[size];
			}
		};
	}

	public static class SurveyResponseFragment extends Fragment {

		// see:http://stackoverflow.com/a/5837927
		static final RadioGroup.OnCheckedChangeListener sToggleListener = new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(final RadioGroup radioGroup, final int i) {
				for (int j = 0; j < radioGroup.getChildCount(); j++) {
					final ToggleButton view = (ToggleButton) radioGroup.getChildAt(j);
					view.setChecked(view.getId() == i);
				}
			}
		};

		public SurveyResponseFragment() {
		}

		@Override
		public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_survey, container, false);
			for (int radioGroup : RESPONSE_GROUPS) {
				RadioGroup parent = rootView.findViewById(radioGroup);
				parent.setOnCheckedChangeListener(sToggleListener);
			}
			return rootView;
		}
	}
}
