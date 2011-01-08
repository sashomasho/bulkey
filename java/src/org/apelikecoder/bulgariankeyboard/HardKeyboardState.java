package org.apelikecoder.bulgariankeyboard;

import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;

/**
 * Class that encapsulates meta states for hardware keyboard.
 * @author spapadim - 
 *
 */
public class HardKeyboardState {
	public static final int META_OFF = 0;
	public static final int META_ON = 1;
	public static final int META_LOCKED = 2;
	
	public static final int META_SHIFT = 0;
	public static final int META_ALT = 1;
	
	private static final int MASK_SHIFT_STATES =
		KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON | KeyEvent.META_SHIFT_RIGHT_ON;
	private static final int MASK_ALT_STATES =
		KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON | KeyEvent.META_ALT_RIGHT_ON;
	private static final int MASK_SYM_STATES =
		KeyEvent.META_SYM_ON;
	private static final int MASK_ALL_STATES = MASK_SHIFT_STATES | MASK_ALT_STATES | MASK_SYM_STATES;

	private static final int[] MASK_STATES = new int[]{ MASK_SHIFT_STATES, MASK_ALT_STATES };
	
	private LatinIME mContext;
	private int[] mMetaStates = new int[2];
	
	public HardKeyboardState (LatinIME context) {
		mContext = context;
	}
	
	public void clearMetaState (int meta) {
		mMetaStates[meta] = META_OFF;
		mContext.getCurrentInputConnection().clearMetaKeyStates(MASK_STATES[meta]);
	}
	
	public void clearAllMetaStates () {
        InputConnection ic = mContext.getCurrentInputConnection();
        if (ic != null) {
        	ic.clearMetaKeyStates(MASK_ALL_STATES);
        }
		for (int i = 0;  i < mMetaStates.length;  i++) {
			mMetaStates[i] = META_OFF;
		}
	}

	int getMetaState (int meta) {
		return mMetaStates[meta];
	}
	
	public boolean isMetaOn (int meta) {
		return mMetaStates[meta] != META_OFF;
	}
	
	public int shiftMetaState (int meta) {
		int nextState = META_OFF;
		switch (mMetaStates[meta]) {
		case META_OFF:
			nextState = META_ON;
			break;
		case META_ON:
			nextState = META_LOCKED;
			break;
		case META_LOCKED:
			nextState = META_OFF;
			break;
		}
		mMetaStates[meta] = nextState;
		return nextState;
	}
	
	/**
	 * Update the meta key state after a key has been pressed.  
	 * If the hardware keypress event won't be propagated further, make sure
	 * to also update the hardware meta state accordingly.
	 * 
	 * @param meta The meta state to update.
	 * @param consumed Whether the keypress will be consumed by the IME, or propagated.
	 *    Should match the return value of {@link android.inputmethod.InputMethodService#onKeyDown}.
	 */
	public void updateMetaStateAfterKeypress (int meta, boolean consumed) {
		if (mMetaStates[meta] == META_ON) {
			mMetaStates[meta] = META_OFF;
			if (consumed) {
				mContext.getCurrentInputConnection().clearMetaKeyStates(MASK_STATES[meta]);
			}
		}
	}
	
	public void updateAllMetaStatesAfterKeypress (boolean consumed) {
		InputConnection ic = mContext.getCurrentInputConnection();
		for (int i = 0;  i < mMetaStates.length;  i++) {
			if (mMetaStates[i] == META_ON) {
				mMetaStates[i] = META_OFF;
				if (consumed) {
					ic.clearMetaKeyStates(MASK_STATES[i]);
				}
			}
		}
	}
}
