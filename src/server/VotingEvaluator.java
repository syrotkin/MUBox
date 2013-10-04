package server;

import java.util.Timer;
import java.util.TimerTask;

import server.data.FileManager;
import server.data.VotingManager;

public class VotingEvaluator {

	private Timer timer;
	private TimerTask task;
	private final VotingManager votingManager;
	private final ChangeManager changeManager;
	private final CloudFactory cloudFactory;
	private final FileManager fileManager;
		
	public VotingEvaluator(VotingManager vm, ChangeManager cm, CloudFactory cf, FileManager fm) {
		this.votingManager = vm;
		this.changeManager = cm;
		this.cloudFactory = cf;
		this.fileManager = fm;
		this.timer = new Timer();
		this.task = new TimerTask() {
			@Override
			public void run() {
				//System.out.println("Evaluating time-constraint votings.");
				votingManager.closeTimeConstraintVotings(changeManager, cloudFactory, fileManager);
			}
		};
	}

	public void start() {
		this.timer.schedule(this.task, 60000, 60000);
	}
	
}
