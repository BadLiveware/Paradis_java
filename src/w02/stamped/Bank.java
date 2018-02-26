// Peter Idestam-Almquist, 2018-02-21.

package w02.stamped;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;

// TODO: Make this class thread-safe and as performant as possible.
class Bank {
	
	class AccountAndLockWrapper {
		Account acc;
		StampedLock accLock;
		
		public AccountAndLockWrapper(Account acc) {
			this.acc = acc;
			accLock = new StampedLock();
		}
		public Account getAccount() { return acc; }
		public StampedLock getLock() { return accLock; }
	}
	// Instance variables.
	private int accountCounter = 0;
	
		// While not covered in the standard test, HashMap is not thread-safe and can break if accounts are added while transaction/operation work is running,
		// ConcurrentHashMap does striped locking and thus does not heavily impact performance unless all read/writes happen in the same stripe.
	private Map<Integer, AccountAndLockWrapper> accounts = new ConcurrentHashMap<>();
	
	// Instance methods.
	
	int newAccount(int balance) {
		int accountId = accountCounter++;
		Account account = new Account(accountId, balance);
		AccountAndLockWrapper alw = new AccountAndLockWrapper(account);
		accounts.put(accountId, alw);
		return accountId;
	}
	
	void runOperation(Operation operation) {
		AccountAndLockWrapper awl = accounts.get(operation.getAccountId());
		StampedLock lock = awl.getLock();
		long stamp = lock.writeLock();
		try {
			Account account = awl.getAccount();
			account.setBalance(account.getBalance() + operation.getAmount());
		} finally {
			lock.unlock(stamp);
		}
	}
		
	// TODO: If you are not aiming for grade VG you should remove this method.
	void runTransaction(Transaction transaction) {
		@SuppressWarnings("unused")
		List<Integer> accountIds = transaction.getAccountIds();
		List<Operation> operations = transaction.getOperations();
		
		for (Operation operation : operations) {
			
			AccountAndLockWrapper alw = accounts.get(operation.getAccountId());
			StampedLock lock = alw.getLock();
			
			long stamp = lock.writeLock();
			try {
				Account acc = alw.getAccount();
				acc.setBalance(acc.getBalance() + operation.getAmount());
			} finally {
				lock.unlock(stamp);
			}
		}
	}
	
	// Not used for anything except printing
	int getAccountBalance(int accountId) {
		AccountAndLockWrapper alw = accounts.get(accountId);
		StampedLock lock = alw.getLock();
		
		long stamp = lock.tryOptimisticRead();
		Account account = alw.getAccount();
		int toReturn = account.getBalance();
		
		if (!lock.validate(stamp)) {
			stamp = lock.readLock();
			try {
				toReturn = account.getBalance();
			} finally {
				lock.unlock(stamp);
			}
		}
		return toReturn;
	}
}