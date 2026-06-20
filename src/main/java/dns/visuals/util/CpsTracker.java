package dns.visuals.util;

import java.util.ArrayDeque;
import java.util.Deque;

/** Counts clicks within the last second for the CPS display. */
public class CpsTracker {
	private final Deque<Long> left = new ArrayDeque<>();
	private final Deque<Long> right = new ArrayDeque<>();

	public void onLeftClick() {
		left.add(System.currentTimeMillis());
	}

	public void onRightClick() {
		right.add(System.currentTimeMillis());
	}

	private int count(Deque<Long> q) {
		long now = System.currentTimeMillis();
		while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
		return q.size();
	}

	public int left() {
		return count(left);
	}

	public int right() {
		return count(right);
	}
}
