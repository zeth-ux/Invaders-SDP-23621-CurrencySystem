package engine;

/**
 * Tracks the player's coin balance and keeps it persisted to disk, so it
 * survives between game launches (unlike score, which resets every run)
 * and is shared by every screen that reads or spends it - the in-game HUD
 * and the shop both go through this single instance.
 *
 * This is intentionally small: item definitions, diamonds and full
 * purchase flows belong to the rest of the Currency System requirements
 * (see teams/GoG.md) and can build on top of this class. Every change to
 * the balance is written straight to disk (via {@link FileManager}) rather
 * than only on exit, so a crash or force-quit can't lose earned coins.
 *
 * @author GoG - Currency System
 */
public final class CurrencyManager {

	/** Singleton instance. */
	private static CurrencyManager instance;

	/** Current coin balance, persisted to disk. */
	private int coins;

	/**
	 * Private constructor, loads whatever balance was last saved.
	 */
	private CurrencyManager() {
		this.coins = FileManager.getInstance().loadCoins();
	}

	/**
	 * Controls access to the currency manager.
	 *
	 * @return shared instance of CurrencyManager.
	 */
	public static CurrencyManager getInstance() {
		if (instance == null)
			instance = new CurrencyManager();
		return instance;
	}

	/**
	 * Adds coins to the current balance, e.g. when the player collects a
	 * dropped coin, and immediately persists the new balance.
	 *
	 * @param amount
	 *            Amount of coins to add. Ignored if not positive.
	 */
	public void addCoins(final int amount) {
		if (amount > 0) {
			this.coins += amount;
			save();
		}
	}

	/**
	 * @return current coin balance.
	 */
	public int getCoins() {
		return this.coins;
	}

	/**
	 * Attempts to spend coins, e.g. for a shop purchase or ship unlock.
	 * Deliberately checks-then-spends atomically so a caller never needs to
	 * call getCoins() first and race against another deduction. Persists
	 * the new balance immediately on success.
	 *
	 * @param amount
	 *            Amount of coins to spend. Must be positive.
	 * @return true if the balance had enough coins and the amount was
	 *         deducted; false if funds were insufficient and nothing
	 *         changed.
	 */
	public boolean trySpend(final int amount) {
		if (amount <= 0)
			throw new IllegalArgumentException("amount must be positive");
		if (this.coins < amount)
			return false;
		this.coins -= amount;
		save();
		return true;
	}

	/**
	 * Resets the balance to zero and persists it. Useful for tests; not
	 * meant to be called during normal play.
	 */
	public void reset() {
		this.coins = 0;
		save();
	}

	/**
	 * Writes the current balance to disk.
	 */
	private void save() {
		FileManager.getInstance().saveCoins(this.coins);
	}
}
