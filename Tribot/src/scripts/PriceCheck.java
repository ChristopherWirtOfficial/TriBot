package scripts;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tribot.api2007.Banking;
import org.tribot.api2007.Interfaces;
import org.tribot.api2007.Inventory;
import org.tribot.api2007.types.RSInterfaceChild;
import org.tribot.api2007.types.RSInterfaceComponent;
import org.tribot.api2007.types.RSInterfaceMaster;
import org.tribot.api2007.types.RSItem;
import org.tribot.api2007.types.RSItemDefinition;
import org.tribot.script.Script;
import org.tribot.script.ScriptManifest;
import org.tribot.script.interfaces.Painting;

@ScriptManifest(authors = { "Wicomb" }, category = "Tools", name = "PriceCheck", description = "Checks the worth of your inventory for you!")
public class PriceCheck extends Script implements Painting {

	private ArrayList<Integer> items = new ArrayList<Integer>();
	private ArrayList<Integer> ids = new ArrayList<Integer>(); // Lazy way out
	private ArrayList<PricedItem> pricedItems = new ArrayList<PricedItem>();

	class PricedItem {

		Rectangle area;
		int price;
		int stack;

		public PricedItem(Rectangle r, int p, int s) {
			area = r;
			price = p;
			stack = s;
		}
	}

	@Override
	public void run() {
		System.out.println("Starting up..");
		int worth = 0;
		RSItem[] inv = Inventory.getAll();
		System.out.println("Got inv");
		if (Interfaces.isInterfaceValid(335)) {
			System.out.println("Trade window open");
			RSInterfaceMaster tInf = Interfaces.get(335);
			ArrayList<RSItem> itemsL = new ArrayList<RSItem>();
			if (tInf != null) {
				RSInterfaceChild tChild = tInf.getChild(50);
				if (tChild != null) {
					RSInterfaceComponent[] itemComps = tChild.getChildren();
					for (RSInterfaceComponent item : itemComps) {
						if (item != null) {
							String name = item.getComponentName();
							if (name != null) {
								// First check if the item is in the list, and
								// if it is
								int price = 0;
								if (!ids.contains(item.getComponentItem())) {
									price = ZybezGuide.getPrice(name);
									ids.add(item.getComponentItem());
									System.out.println(name + " x"
											+ item.getComponentStack() + ": "
											+ price);
									items.add(price);

								} else {
									price = items.get(ids.indexOf(item
											.getComponentItem()));
								}
								PricedItem pricedItem = new PricedItem(
										item.getAbsoluteBounds(), price,
										item.getComponentStack());
								pricedItems.add(pricedItem);
								worth += price * item.getComponentStack();
							}
						}
					}
				}
			}
		} else
			for (RSItem item : inv) {
				if(item == null)
					continue;
				RSItemDefinition def = item.getDefinition();
				if(def == null)
					continue;
				String name = def.getName();
				if (item != null && name != null) {
					// First check if the item is in the list, and if it is
					int price = 0;
					if (!ids.contains(item.getID())) {
						price = ZybezGuide.getPrice(name);
						ids.add(item.getID());
						System.out.println(name + " x" + item.getStack() + ": "
								+ price);
						items.add(price);
						PricedItem pricedItem = new PricedItem(item.getArea(),
								price, item.getStack());
						pricedItems.add(pricedItem);
					} else {
						price = items.get(ids.indexOf(item.getID()));
					}
					worth += price * item.getStack();
				}
			}
		if (Banking.isBankScreenOpen()) {
			System.out.println("Bank is open, doing that too");
			RSItem[] bank = Banking.getAll();
			for (RSItem item : bank) {
				if(item == null)
					continue;
				String name = item.getDefinition().getName();
				if (item != null && name != null) {
					
					// First check if the item is in the list, and if it is
					int price = 0;
					if (!ids.contains(item.getID())) {
						price = ZybezGuide.getPrice(name);
						ids.add(item.getID());
						System.out.println(name + " x" + item.getStack() + ": "
								+ price);
						items.add(price);
					} else {
						price = items.get(ids.indexOf(item.getID()));
					}
					worth += price * item.getStack();
				}
			}
			// Interface number 335, child 50, all components
		}
		System.out.println("Worth: " + worth);
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static class ZybezGuide {

		private static String source;

		private static String webpage = "http://forums.zybez.net/runescape-2007-prices/api/item/";

		public static int getPrice(String name) {
			if (name.equals("Coins"))
				return 1;
			try {
				source = readUrl(webpage + name.replaceAll(" ", "+"));
				if (!source.contains("error")) {
					if (countOccurrences(source, '{') < 6)
						return 1;
					Pattern pattern = Pattern
							.compile("(?<=\"average\":\")[0-9]+");
					Matcher matcher = pattern.matcher(source);
					while (matcher.find()) {
						return Integer.parseInt(matcher.group());
					}
				} else {
					return 0;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return 0;
		}

		private static String readUrl(String urlString) throws Exception {
			BufferedReader reader = null;
			StringBuffer buffer = null;
			URLConnection uc = null;
			String cl;

			try {
				URL url = new URL(urlString);
				uc = url.openConnection();
				uc.addRequestProperty("User-Agent",
						"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
				uc.connect();
				reader = new BufferedReader(new InputStreamReader(
						uc.getInputStream()));
				buffer = new StringBuffer();

				while ((cl = reader.readLine()) != null)
					buffer.append(cl);

				return buffer.toString();
			} finally {
				reader.close();
			}
		}

	}

	public static int countOccurrences(String haystack, char needle) {
		int count = 0;
		for (int i = 0; i < haystack.length(); i++) {
			if (haystack.charAt(i) == needle) {
				count++;
			}
		}
		return count;
	}

	@Override
	public void onPaint(Graphics g) {
		g.setFont(new Font("default", Font.BOLD, 12));
		for (PricedItem item : pricedItems) {
			if(item.price == 0)
				continue;
			int p = item.price;
			int s = p * item.stack;
			String price = p + "";
			String stack = s + "";
			if (p > 999999) {
				price = Math.floor(p / 10000) / 100000D + "m";
			} else if (p > 999) {
				price = Math.floor(p / 10) / 100D + "k";
			}
			if (s > 999999) {
				stack = Math.floor(s / 10000) / 100000D + "m";
			} else if (s > 999) {
				stack = Math.floor(s / 10) / 100D + "k";
			}
			g.setColor(new Color(255, 255, 255, 144));
			g.fillRect(item.area.x, item.area.y, item.area.width,
					item.area.height);
			g.setColor(Color.black);
			g.drawString(price, item.area.x + 2, item.area.y + 14);
			if(item.stack > 1)
				g.drawString(stack, item.area.x + 2, item.area.y + 24);
		}
	}
}
