package org.bofus.sumcompare.localutil;

import java.util.Scanner;

import org.bofus.sumcompare.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserUtilities
{
	private static final Logger logger = LoggerFactory.getLogger(UserUtilities.class);

	public static boolean getUserAcceptance()
	{
		boolean returnValue = false;
		
		Scanner inputReader = new Scanner(System.in);
		String userInput = null;

		System.out.println("-----------------------------------------------------------------------------------------------------------------------------\r\n");
		System.out.println("NOTE: Please ensure you have created backups of the source and destination prior to running this\r\n");
		System.out.println("No Warranties:\r\n" + 
				"The creator of this application  (from here forth, \"We\") make no express warranty/guarantee regarding this application and disclaim any implied warranty and guarantee.\r\n"
				+ "We do not authorize anyone to make any warranties on our behalf and you should not rely on any such statement.\r\n" + 
				"\r\n" + 
				"Limitation On Liability:\r\n" + 
				"In no event will we be liable for any damages including, without limitations, incidental or consequential damages and damages\r\n"
				+ "for lost data or profits, even if we have been advised of the possibility of such damages.\r\n" + 
				"\r\n" + 
				"You Agree With User Acceptance Policy:\r\n" + 
				"By answering 'YES', you agree that you agree to the terms and conditions as stated above.\r\n");
		System.out.println("-----------------------------------------------------------------------------------------------------------------------------\r\n");
		System.out.println("Do you agree? (you must type the word YES to continue):");
		
		userInput = inputReader.nextLine();
		
//		StringBuilder stringBuilder = new StringBuilder();
//		stringBuilder.append(str)
		
		if (userInput.equalsIgnoreCase("YES"))
		{
			returnValue = true;
		}
		else
		{
			System.out.println(String.format("You answered %s to the acceptance, this process will terminate now", userInput));
			System.exit(99);
		}
		return returnValue;
	}
}
