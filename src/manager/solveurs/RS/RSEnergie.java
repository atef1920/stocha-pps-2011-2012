package manager.solveurs.RS;

import java.util.Random;

import data.DataBinaire;
import data.solution.Solution;
import data.solution.SolutionEnergieBinaire;

/**
 * Impl�mentation sp�cialis�e du recuit simul� pour le probl�me de management de la production d'�nergie.
 * @author Fabien BINI & Nathana�l MASRI & Nicolas POIRIER
 */
public class RSEnergie extends RecuitSimule
{
	/** Les donn�es du probl�me */
	private DataBinaire donnees;
	/** Le nombre d'it�rations par palier de temp�rature */
	private int nbIterationsParPalier;
	/** Le nombre d'it�rations courant pour le palier de temp�rature courant.*/
	private int iterationCourante;
	/** La temp�rature � atteindre pour arr�ter le recuit. */
	private double temperatureFinale;
	/** Le nombre de modifications pour lesquelles on garde le m�me sc�narios */
	private int nbTransformationsParScenarios;
	/** Le nombre de transformations imposibles avant de changer de sc�narios */
	private int nbTestsTransformations;
	/** Le nombre courant de modifications pour lesquelles on garde le m�me sc�narios */
	private int nbTransformationsParScenariosCourant;
	
	/**
	 * Construit un recuit simul� sp�cialis� pour le probl�me de la p-m�diane.
	 * @param donnees les donn�es du probl�me
	 * @param facteurDecroissance le facteur de d�croissance de la temp�rature du recuit.
	 * @param temperatureFinale la temp�rature � atteindre pour arr�ter le recuit.
	 * @param nbIterationsParPalier le nombre d'it�rations par palier
	 * @param tauxAcceptation le taux d'acceptation de solutions co�teuses accept�es par le recuit � la temp�rature initiale
	 * @param nbTransformationsParScenarios le nombre de modifications pour lesquelles on garde le m�me sc�narios
	 * @param nbTestsTransformations le nombre de transformations imposibles avant de changer de sc�narios
	 */
	public RSEnergie(DataBinaire donnees, double facteurDecroissance, double temperatureFinale, int nbIterationsParPalier, double tauxAcceptation, int nbTransformationsParScenarios, int nbTestsTransformations)
	{
		super(facteurDecroissance, tauxAcceptation);
		solutionCourante = new SolutionEnergieBinaire(donnees);
		meilleureSolution = new SolutionEnergieBinaire(donnees);
		this.temperatureFinale = temperatureFinale;
		this.donnees = donnees;
		this.nbIterationsParPalier = nbIterationsParPalier;
		this.nbTestsTransformations = nbTestsTransformations;
		this.nbTransformationsParScenarios = nbTransformationsParScenarios;
		iterationCourante = 0;
		nbTransformationsParScenariosCourant = 0;
	}

	/**
	 * Teste si le recuit est arriv� la temp�rature finale demand�e � la cr�ation.
	 * @return true si le recuit peut passer au palier de temp�rature suivant, false sinon.
	 */
	protected boolean testerCondition1()
	{
		if(temperature > temperatureFinale)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Teste si le recuit a encore des it�rations � faire pour un palier de temp�rature.
	 * @return true si le recuit doit continuer � ce palier de temp�rature, false sinon.
	 */
	protected boolean testerCondition2()
	{
		iterationCourante++;
		
		if(iterationCourante < nbIterationsParPalier)
		{
			return true;
		}
		else
		{
			iterationCourante = 0;
			return false;
		}
	}

	/**
	 * S�lectionne une solution dans le voisinage de la solution courante en modifiant l'activation d'un sc�nario et une d�cision.
	 * Un sc�nario actif devient inactif et inversement. Il faut que la nouvelle configuration ait une probabilit� sup�rieure � celle demand�e.
	 * Une d�cision passe de 0 � 1 ou de 1 � 0. Il faut que la nouvelle configuration respecte l'offre et la demande et les contraintes d'unicit�es.
	 * @return une solution voisine de la solution courante.
	 */
	protected Solution voisin()
	{
		// La solution voisine est dans un premier temps une copie de la solution courante
		SolutionEnergieBinaire solution = (SolutionEnergieBinaire) solutionCourante.clone();
		
		Random rand = new Random();
		int nbTests; // Le nombre de test de solution pour un jeu de sc�narios
		boolean recherche;
		nbTransformationsParScenariosCourant++;
		
		do
		{
			// Active ou desactive un sc�nario si c'est le moment
			if(nbTransformationsParScenariosCourant >= nbTransformationsParScenarios)
			{
				nbTransformationsParScenariosCourant = 0;
				int indicePeriodeChange;
				int indiceScenarioChange;
				// On essaie des modifications tant que �a ne r�pond pas � la probabilit� voulue
				do
				{
					recherche = false;
					indicePeriodeChange = rand.nextInt(solution.getZ().length);
					indiceScenarioChange = rand.nextInt(solution.getZ()[indicePeriodeChange].length);
					solution.active(indicePeriodeChange, indiceScenarioChange, !solution.isActived(indicePeriodeChange, indiceScenarioChange));
					
					// Repasse dans l'�tat initial si �a ne r�pond pas � la probabilit� voulue
					if(solution.probabiliteScenario(indicePeriodeChange) < donnees.getProbabilite(indicePeriodeChange))
					{
						solution.active(indicePeriodeChange, indiceScenarioChange, !solution.isActived(indicePeriodeChange, indiceScenarioChange));
						recherche = true;
					}
					
				} while(recherche);
			}
			
			// Essaie de modifier les d�cisions tant que �a ne r�pond pas � la demande
			nbTests = 0;
			do
			{
				recherche = false;
				nbTests++;
				
				// On modifie le palier d'une centrale pour une p�riode
				// Il faut choisir entre modifier le choix d'un palier thermique ou bien la trajectoire
				// Cela se d�cide en fonction du nombre de paliers et du nombre de trajectoires
				double proba = (donnees.nbCentrales * donnees.nbPeriodes) / (double) (donnees.nbCentrales * donnees.nbPeriodes + donnees.nbTrajectoires);
				
				// On modifie un choix de palier
				if(rand.nextDouble() < proba)
				{
					int periodeChange;
					int centraleChange;
					int palierChange;
					// Choix de la p�riode � modifier
					periodeChange = rand.nextInt(donnees.nbPeriodes);
					// choix de la centrale thermique � modifier
					centraleChange = rand.nextInt(donnees.nbCentrales);
					// L'ancienne valeur est sauvegard�e
					int ancienPalier = solution.getDecisionPeriodeCentrale(periodeChange, centraleChange);
					// On tire le niveau choix de palier, il doit �tre diff�rent de l'ancien
					do
					{
						palierChange = rand.nextInt(donnees.nbPaliers[centraleChange]);
					} while(palierChange == ancienPalier);
					
					// On modifie la solution
					solution.setDecisionPeriodeCentrale(periodeChange, centraleChange, palierChange);
					
					// Si la solution ne repond pas � la demande, on repasse dans l'�tat initial et on refait une modification
					if(!solution.respecteContrainteDemandePeriode(periodeChange))
					{
						recherche = true;
						solution.setDecisionPeriodeCentrale(periodeChange, centraleChange, ancienPalier);
					}
				}
				// On modifie la trajectoire hydraulique
				else
				{			
					int trajectoireChange;
					// On sauvegarde l'ancienne trajectoire
					int ancienneTrajectoire = solution.getTrajectoire();
					// On tire une trajectoire, elle doit �tre diff�rente de l'ancienne
					do
					{
						trajectoireChange = rand.nextInt(donnees.nbTrajectoires);
					} while(trajectoireChange == solution.getTrajectoire());
					
					// Modifie la trajectoire
					solution.setTrajectoire(trajectoireChange);
					
					// Si �a ne r�pond pas � la demande, on repasse dans l'�tat initial et on refait une modification
					if(!solution.respecteContrainteDemande())
					{
						recherche = true;
						solution.setTrajectoire(ancienneTrajectoire);
					}
				}
			} while(recherche && nbTests < nbTestsTransformations);
			// On refait une modification si �a ne r�pond pas � la demande et que le nombre maximum de modifications par modification de sc�narios n'est pas atteint
		} while(recherche && nbTests == nbTestsTransformations); // On modifie les scenarios si aucune solution n'a �t� trouv�e
		
		return solution;
	}
	
	public static void main(String[] args)
	{
		DataBinaire data = new DataBinaire("Data/Donn�es_Recuit_demandes.csv", "Data/Donn�es_Recuit_paliers1.csv", "Data/Donn�es_Recuit_paliers2.csv", "Data/Donn�es_Recuit_paliers3.csv", "Data/Donn�es_Recuit_paliers4.csv", "Data/Donn�es_Recuit_trajectoire_hydro.csv", "Data/Donn�es_Recuit_parametres_hydro.csv", "Data/Donn�es_Recuit_capacit�.csv");
		RSEnergie rs = new RSEnergie(data, 0.9, 0.01, 16384, 0.8, 10, 100);
		rs.lancer();
		SolutionEnergieBinaire solution = (SolutionEnergieBinaire) rs.getSolution();
		System.out.println(solution);
	}
}
