package com.clanclogclient;

import java.util.UUID;
import lombok.Value;

@Value
class Submission
{
	UUID idempotencyKey;
	String username;
	String clanName;
	int itemId;
	String itemName;
	String worldType;
	String chatMessage;
	boolean personal;
	boolean lootEvent;
}
