#
# do 250 repeats of the following
#
# do in groups of 10, so the images are varied
#
$position = 1;
$image = 1;
for ($group = 1 ; $group <= 10 ; $group++)
{
###for ($outer = 1 ; $outer <= 25 ; $outer++)
for ($outer = 1 ; $outer <= 22 ; $outer++)
{
print "SECTION:shows". $position . "::On the Show Circuit::On the Show Circuit\n";

print "P:  by Alan Warren \n";
print "P:  repeat number $position \n";
print "P:  [Note: The purpose of this column is to bring to the attention of  MPHS members the awards obtained in recent shows for exhibits that are basically about military postal history. This may include exhibits by non-members. While there are many non-military related exhibits by members, these are not recorded  here.] \n";
print "P:\n";
print "B:Ed Dubin \n";
print "took a gold at the Plymouth Show in  Michigan in April with his \"Civilians, German and Austro-Hungarian POWs Interned in the U.S. during World War  1.\" At the same show \n";
print "B:Andrew Mazzara \n";
print "received a vermeil  and an AAPE award of honor for \"British and Guernsey  Stamps and their Use during German Occupation 1940-1945.\" \n";
print "P:\n";
print "B:Richard Wilson \n";
print "had two entries in the Philatelic Show held in Boxborough, Massachusetts in May. His  \"British Forces in Egypt: 1932-1936 -- The Postal Concession\" received a gold and the MPHS award. Wilson's  \"British Forces in Egypt: 1936-1941 -- The Postal Concession\" won a vermeil. \n";
print "P:At the same show \n";
print "B:Sandeep Jaiswal \n";
print "won a gold and  best single frame exhibit for his \"Indian Postal Stationery  for the China Expeditionary Force.\" Another gold along  with the U.S. Philatelic Classics Society award went to \n";
print "B:Anthony Dewey \n";
print "for  \"The War  Rate 1815-1816.\" \n";
print "P: Several military related exhibits were in competition at ORAPEX in Ottawa in May. \n";
print "B:J. Michael Powell \n";
print " won a gold and the Postal History Society of Canada's E.  R. Toop best military postal history award for his  \"Interned in Canada.\" \n";
print "B:Doreen Fitzgerald \n";
print "also received a  gold, the AAPE creativity award, the BNAPS best 2-4  frame award, and the Wally Gutzman postcard award with  her \"World War I -- Messages Home in Silk.\"  \n";
print "P: Still another gold went to \n";
print "B:Michele Cartier \n";
print "for \"The  French Revolution and the Beginning of the Revolutionary  Wars in Europe, 1789-1796.\" \n";
print "B:Chris Anstead \n";
print "took a silver  for \"The Royal Flying Corps based in Deseronto 1917-  1918 Per Ardua ad Astra.\" A regional level silver went to \n";
print "B:Robert Benoit \n";
print "for  \"The Concept of France under  Occupation 1940-1944.\" \n";
print "P:At the Rocky Mountain Stamp Show in Denver in  May, \n";
print "B:Giorgio Migliavacca \n";
print "won a gold and the MPHS  award for \"The Diaspara of the Italian Prisoners of War  Captured in Africa 1940-1946.\" \n";
print "B:Stanley Luft \n";
print "received a  vermeil for \"French 20th Century Military Postal Markings.\" \n";
print "B:David and Laurie Bernstein \n";
print "also received vermeils  for \"A Postal History of the German Battleship Bismarck\"  and \"Taffy 3 -- Two Hours of Guts and Gumption.\" \n";
print "B:Myron Palay \n";
print "won a vermeil and the AAPE novice award at  Ropex in Canandaigua NY in May for his \"Sino-Russo-Japanese War Military Mail: Manchuria, Korea, Japan  1894-1911.\"  \n";
print "P:Two military related exhibits were shown at the  Royal-2015-Royale show of the Royal Philatelic Society  of Canada held in London, Ontario in May. \n";
print "B:Michael Deery \n";
print " received a gold, the AAPE creativity award, and the Auxiliary Markings Club award for \"WW II Postal Violations  and Warnings.\" At the same show \n";
print "B:John Hall \n";
print "won a vermeil and the best BNAPS 2-4 frame exhibit award with his  \"The Canadian Fiscal War Tax Stamps of WW I.\" \n";
print "P:\n";
print "B:Henry Laessig \n";
print "received a gold for  \"Feldpost  Expositur Cancels of the 1879-1908 Austrian Occupation  of Novi Pazar\" at NOJEX in Seacucus, N.J. in May. A vermeil and the American Helvetia Philatelic society's best  one frame award went to \n";
print "B:Michael Peter \n";
print "for \"Swiss Volunteers in the German Military during WW II.\" Another vermeil went to \n";
print "B:Tony Dewey \n";
print "for \"Service of Intellectual Aid  to Prisoners of War,\" and a silver to \n";
print "B:Zachary Simmons \n";
print "for  \"Italian and French Forces in the Holyland 1917-1921.\" \n";
print "P:\n";
print "B:Alexander Kolchinsky \n";
print "won a gold and the Auxiliary Markings Club award at NAPEX in McLean, Va., in  June with his \"Prisoners of the Great War Send Home Picture Postcards.\" Another gold along with the American Air  Mail Society gold and the MPHS award went to '\n";
print "B:Anne G.  Lowfyle\n";
print "' for  \"British WWII Prisoners of War  Stationery.\" \n";
print "B:Norval Rasmussen \n";
print "received a gold at the OKPEX  show in Midwest City, Oklahoma in June with his  \"Tunisia World War II to Independence 1939-1956.\" \n";
print "P:\n";
print "B:James C. Cate \n";
print "won not only a gold but also the  best single frame award at COLOPEX in Columbus, Ohio  in June with his \"Confederate Military Mail -- Chattanooga  1862-1864.\" \n";
print "IM:x1000/x1000_" . $group . ".jpg::imxx" . $image . "\n";
print "Example image $image\n";
$image++; ## must be unique
print "IM:x1000/x1000_" . $group . ".jpg::imxx" . $image . "\n";
print "Example image $image\n";
$image++; ## must be unique
print "SECTION:militaria" . $position . "::Philatelic Militaria::Philatelic Militaria\n";
print "P:  by Alan Warren \n";
print "P:  repeat number $position \n";
print "IN:American Philatelic Research Library\n";
print "P: [The following articles appeared in recent issues of a variety of journals and may be of interest to military postal history collectors. Copies of the complete articles can usually be obtained through the American Philatelic Research Library, 100 Match Factory Place, Bellefonte PA 16823.] \n";
print "IN:German East Africa\n";
## each unique
print "A:hoffman" . $image . "\n";
print "P:Regis Hoffman continues his series on the \n";
print "B: Allied occupation of German East Africa, 1914-1920\n";
print ", in the April \n";
print "I:German Postal Specialist\n";
print ". He concludes his discussion on the Belgian forces and continues with details on South African forces, German internees and POWs, and the British civilian control of GEA to replace military control. GEA was then partitioned by the League of Nations to Britain (Tanganyika), Belgium (Ruanda, Urundi), and Portugal (Kionga). \n";
print "IN:Russia\n";
print "P:Vladimir Berdichevsky focuses on the \n";
print "B:historic Russian squadron's movement \n";
print "from the Baltic Sea to the Far East 1904-1905 in the Spring issue of \n";
print "I:Rossica\n";
print ", the journal of the Rossica Society of Russian Philately. The Russian fleet in the East was no match for Japan's. Ships from the Baltic were hastily repaired and overhauled and some vessels were sent around Africa's Cape of Good Hope while others went via the Mediterranean and the Suez Canal. Surviving mail and diaries of sailors detail the long journey. Letters and cards were sent via consulates in locations where the ships re-coaled. The long journey ended when the Japanese fleet attacked the Russians in the Tsushima Strait near Korea. The result was the sinking of two-thirds of the Russian fleet, the capture of six vessels, and the flight of others to neutral ports. The Treaty of Portsmouth (N.H.) ended the Russo-Japanese war. \n";
print "P:In the March newsletter of the Canadian Military Mail Study Group, Alan Baker shows a cover sent from the \n";
print "B:Jamaica \"Y\" Force (Winnipeg Grenadiers)\n";
print " and examined by censor Capt. William Askey. He asks what happened to him as Baker had heard that one censor there, Major Hook, died in a Japanese prisoner camp. In the same issue Jim Felton furnishes some \n";
print "B:new dates of WW I internment censor markings \n";
print "used at Amherst, Nova Scotia; Kingston, Ontario; and Vernon, British Columbia. \n";
print "IN:Poland\n";
print "P:A. Fritz describes \n";
print "B:postcards sent between Polish displaced persons in Germany after World War II\n";
print " in the fourth quarter 2014 issue of \n";
print "I:Possessions\n";
print ", published by the United States Possessions Philatelic Society. An inter-camp post was developed for Polish refugees including special stamps which were sold for charity purposes. Following the mass repatriation of Poles in 1946-1947, the remaining people were told to use the AMG post. \n";
print "IN:Women in Military\n";
print "P:Jonathan Johnson shows some \n";
print "B:mail from a Nursing Sister--commissioned women officers in the Canadian Army Medical Corps \n";
print "who wore uniforms in WW I--in the April-June issue of \n";
print "I:BNA Topics\n";
print ". The covers reveal a variety of APO and censorship markings. In the same issue Gordon McDermid describes a \n";
print "B:Canadian Overseas Expeditionary Force marking \n";
print "applied in France during WW I on a letter sent to Toronto. The marking contains the spelling error, EXEPDITIONARY. \n";
print "P:Ken Lawrence writes about \n";
print "B:World War II women in the military and their mail \n";
print "in the May 2015 \n";
print "I:Linn's Stamp News\n";
print ". In this first part he describes and illustrates examples of mail sent by the \n";
print "B:Women's Army Auxiliary Corps (WAAC, later the Women's Army Corps or WAC), and the Women's Airforce Service Pilots (WASP)\n";
print ". The concluding part appeared in the June 2015 issue and addresses mail of the \n";
print "B:WAVES (Women Accepted for Volunteer Emergency Service)\n";
print " for  the Navy and also the Coast Guard and Marines. He discusses the benefits beyond basic pay like provision for dependent spouses and other family members including children. Some examples of mail are also shown. \n";
print "P:Anthony Fandino describes the \n";
print "B:early battles between the British and French Navies in WW II in the \n";
print "June \n";
print "I:U.S. Stamp News\n";
print ". In 1943 French naval forces joined with the Allied cause and French vessels were sent to American ports to be refitted and updated. French sailors' RF mail using U.S. postage in 1944 is mentioned and an example of a cover is shown. \n";
print "P:Larry Nelson writes about the \n";
print "B:German WW I internment camp in Tost, Poland\n";
print ", in the May \n";
print "I:German Postal Specialist\n";
print ". Established first for POW officers in July 1940 it was converted to an internee camp Ilag Tost in October of that year. He shows some examples of mail to and from the camp. \n";
print "P:Canada's War Savings Stamps is the subject of the May issue of \n";
print "I:War Times \n";
print "published by the BNAPS World War II Study Group. A table is shown for the excise war taxes collected annually from 1941 through 1946 by province and by commodity. An index is also provided for war savings stamps articles that have previously appeared in the journal. \n";
print "P:\n";
print "B:The first eight numbered military postal stations in Puerto Rico \n";
print "are discussed in Frank Avecedo's article in the Second Quarter issue of \n";
print "I:Possessions\n";
print ", published by the United States Possessions Philatelic Society. He describes the stations and shows an example of mail from each one. \n";
print "P:The Yangtze River Patrol Boats are discussed by Anthony Fandino in the July \n";
print "I:U.S. Stamp News\n";
print ". He describes their construction so they could serve in shallow areas upriver, and focuses on the USS \n";
print "I:Luzon\n";
print ". She was eventually scuttled in Manila Bay and later salvaged by the Imperial Japanese Navy as the \n";
print "I:Karatsu\n";
print ", refitted as a sub-chaser and based at Cebu. She suffered major damage from a USS \n";
print "I:Narwhal \n";
print "torpedo but was never repaired. \n";
print "P:Erik L(o)rdahl discusses \n";
print "B:Operation Torch--World War II\n";
print " British-American invasion of North Africa--in the June \n";
print "I:Norwegian War and Field Post Journal\n";
print ". He focuses on the civil post and begins with a cover sent from Algeria in 1943 to the International Red Cross in Geneva. It bears markings that it was examined by Algeria, the British, Germany (Paris A.x. passmark) and a U.S. base camp. \n";
print "P:Last paragraph\n";
## each unique
print "S:hoffman" . $image . ":: see Hoffman article\n";
print "before leaving.\n";
print "IM:x1000/x1000_" . $group . ".jpg::imxx" . $image . "\n";
print "Example image $image\n";
$image++; ## must be unique
print "IM:x1000/x1000_" . $group . ".jpg::imxx" . $image . "\n";
print "Example image $image\n";
$image++; ## must be unique
## end outer
$position++;
}
# end group
}
