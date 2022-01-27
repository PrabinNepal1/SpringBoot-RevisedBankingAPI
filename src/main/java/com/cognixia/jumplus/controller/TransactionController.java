package com.cognixia.jumplus.controller;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cognixia.jumplus.repository.AccountRepository;
import com.cognixia.jumplus.repository.TransactionRepository;
import com.cognixia.jumplus.model.Account;
import com.cognixia.jumplus.model.Transaction;

@RequestMapping("/api/transaction")
@RestController
public class TransactionController {
	
	@Autowired
	TransactionRepository repo;
	
	@Autowired
	AccountRepository accRepo;
	// Get five most recent transactions by an account id
	
	@GetMapping("/five_recent/{id}")
	public List<Transaction> getFiveTransactions(@PathVariable long id) throws Exception {
		
		return repo.getFiveMostRecentTransactions(id);
		
	}
	
	// Add a new transaction with a given account id
	
	@PostMapping("/add")
	public ResponseEntity<?> addTransaction(@RequestBody Transaction trans) {
		
		trans.setId(0L);
		
		Transaction newTrans = repo.save(trans);
		
		return ResponseEntity.status(201).body(newTrans);
	}
	
	// Get a transaction with an id
	
	@GetMapping("/{id}")
	public Transaction getTransactionById(@PathVariable long id) throws Exception {
		
		Optional<Transaction> found = repo.findById(id);
		if(found.isPresent()) {
			return found.get();
		}
		throw new Exception("Transaction with id of '" + id + "' not found.");
		
	}
	
	// Transfer funds from one account to the other
	
	@PutMapping("/transfer")
	public ResponseEntity<?> addTransaction(@RequestParam String fromAcc, @RequestParam String toAcc, @RequestParam Double amount) throws Exception {
		
		// Find the two accounts associated with the transfer
		
		Optional<Account> fromAccount = accRepo.findByUsername(fromAcc);
		Optional<Account> toAccount = accRepo.findByUsername(toAcc);
		
		if(fromAccount.isEmpty()) {
			throw new Exception("Could not find account '" + fromAcc + "'.");
		}
		if(toAccount.isEmpty()) {
			throw new Exception("Could not find account '" + toAcc + "'.");
		}
		
		Account toWithdraw = fromAccount.get();
		Account transferTo = toAccount.get();
		
		// Add the new transactions to both accounts
		
		Transaction transfer1 = new Transaction(0L, "Funds Transfer", "Funds transfer to account '" + toAcc + "'.", amount * -1.0, new Date(), toWithdraw);
		Transaction transfer2 = new Transaction(0L, "Funds Transfer", "Funds transfer from account '" + fromAcc + "'.", amount, new Date(), transferTo);
		
		repo.save(transfer1);
		repo.save(transfer2);
		
		toWithdraw.setBalance(toWithdraw.getBalance() - amount);
		transferTo.setBalance(transferTo.getBalance() + amount);
		
		// Save the funds transfer
		
		toWithdraw.attachTransactions();
		transferTo.attachTransactions();
		
		accRepo.save(toWithdraw);
		accRepo.save(transferTo);
		
		return ResponseEntity.status(201).body(transfer2);
	}

}