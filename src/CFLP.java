//package ad2.ss17.cflp;

import java.util.Arrays;
import java.util.Collections;
import java.util.TreeSet;

/**
 * Klasse zum Berechnen der Loesung mittels Branch-and-Bound.
 * TODO I should really add some documentation.
 */
public class CFLP extends AbstractCFLP
{
    private class Element implements Comparable<Element>
    {
        int weight;
        int id;



        Element(int weight, int elementID)
        {
            this.weight = weight;
            this.id = elementID;
        }



        @Override
        public int compareTo(Element other)
        {
            if(this.weight == other.weight)
            {
                return 0;
            }
            else if(this.weight > other.weight)
            {
                return 1;
            }
            else
            {
                return -1;
            }
        }
    }


    private final int numOfCustomers;
    private final int numOfFacilites;
    private final int distanceCosts;

    private final int[] baseOpeningCosts;
    private final int[] demandedBandwidths;
    private final int[] maxBandwidths;
    private final int[][] distances_facilitiesToCustomers; // [first index] = facility, [second] = customer

    private Element[][] customersFacilites; // [first index] = customers, [second] = facility, Element.weight && Element.id
    private Element[] customers_ordered;

    private int[] solution; // used for setSolution; [index] = customer, value = facility
    private int[] capacities;
    private int upperBound;
    private int lowerBound;



    public CFLP(CFLPInstance instance)
    {
        // TODO: Hier ist der richtige Platz fuer Initialisierungen

        this.numOfCustomers = instance.getNumCustomers();
        this.numOfFacilites = instance.getNumFacilities();
        this.distanceCosts = instance.distanceCosts;

        this.baseOpeningCosts = instance.openingCosts;
        this.demandedBandwidths = instance.bandwidths;
        this.maxBandwidths = instance.maxBandwidths;
        this.distances_facilitiesToCustomers = instance.distances;

        this.customersFacilites = new Element[this.numOfCustomers][];
        this.customers_ordered = new Element[this.numOfCustomers];


        this.solution = new int[this.numOfCustomers];       // used as solution for the setSolution method
        this.capacities = Arrays.copyOf(this.maxBandwidths, this.numOfFacilites);

        this.upperBound = Integer.MAX_VALUE;
        this.lowerBound = 0;

        Arrays.fill(this.solution, -1);


        for(int customer = 0; customer < this.numOfCustomers; customer++)           // go through all customers
        {
            Element[] facilitesForCustomer = new Element[this.numOfFacilites];      // create a 1-dim Element array for each customer

            for(int facility = 0; facility < this.numOfFacilites; facility++)       // add distances and indices of all facilites to that array
            {
                facilitesForCustomer[facility] = new Element(this.distances_facilitiesToCustomers[facility][customer], facility);
            }
            Arrays.sort(facilitesForCustomer);                              // sort array with ascending weight
            this.customersFacilites[customer] = facilitesForCustomer;       // add array to 2-dim facilites array

            this.customers_ordered[customer] = new Element(this.demandedBandwidths[customer], customer);
        }
        Arrays.sort(this.customers_ordered, Collections.reverseOrder());
    }



    /**
     * Diese Methode bekommt vom Framework maximal 30 Sekunden Zeit zur
     * Verfuegung gestellt um eine gueltige Loesung
     * zu finden.
     * Fuegen Sie hier Ihre Implementierung des Branch-and-Bound-Algorithmus
     * ein.
     */
    @Override
    public void run()
    {
        // TODO: Diese Methode ist von Ihnen zu implementieren
        this.branchAndBound(0);
        //this.branchAndBound(this.numOfFacilites - 1);
    }



    private void branchAndBound(int customer)
    {
        if(customer >= 0 && customer < this.numOfCustomers)
        {
            for(int facility = 0; facility < this.numOfFacilites; facility++)   // gehe jede facility durch
            {
                int currentCustomer = this.customers_ordered[customer].id;      // customers nach absteigender BW anfoderung sortiert

                this.solution[currentCustomer] = this.customersFacilites[customer][facility].id;

                if(customer + 1 < this.numOfCustomers)
                {
                    int temp = 0;     // CHANGE this as NEEDED

                    for(int i = 0; i < this.numOfCustomers; i++)
                    {
                        if(solution[i] < 0)
                        {
                            int closest = this.customersFacilites[i][0].id;

                            temp += this.distances_facilitiesToCustomers[closest][i] * this.distanceCosts;
                        }
                    }
                    this.lowerBound = temp + this.calcObjectiveValue();
                }

                if(this.lowerBound < this.upperBound) branchAndBound(customer + 1);

                // Here everything is reset
                this.solution[currentCustomer] = -1;
                this.capacities[facility] += this.demandedBandwidths[currentCustomer];
            }
        }
        else
        {
            int currentCosts = calcObjectiveValue();

            if(currentCosts < this.upperBound)
            {
                this.upperBound = currentCosts;
                setSolution(this.upperBound, this.solution);
            }
        }
    }



    private int calcObjectiveValue()
    {
        boolean[] openedFacilities = new boolean[this.numOfFacilites];
        Arrays.fill(openedFacilities, false);


        if(solution.length != this.numOfCustomers)
            throw new RuntimeException("Problem beim Ermitteln des Zielfunktionswertes (zu wenige/zu viele Kunden)");

        int[] requiredBandwidth = new int[this.numOfFacilites];


        for(int customer = 0; customer < solution.length; ++customer)
        {
            if(solution[customer] < 0) continue;
            requiredBandwidth[solution[customer]] += this.demandedBandwidths[customer];
        }

        int sumCosts = 0;
        for(int customer = 0; customer < solution.length; ++customer)
        {
            if(solution[customer] < 0) continue;

            if(!openedFacilities[solution[customer]])
            {
                sumCosts = Math.addExact(sumCosts, this.calcConstructionCostsFor(this.solution[customer]));
                openedFacilities[solution[customer]] = true;
            }
            sumCosts = Math.addExact(sumCosts, distanceCosts * this.distances_facilitiesToCustomers[solution[customer]][customer]);
        }

        return sumCosts;
    }



    private int calc_CurrentNetworkCosts()
    {
        int costs = 0;
        TreeSet<Integer> facilitesToBeConstructed = new TreeSet<>();


        for(int customer = 0; customer < this.numOfCustomers; customer++)
        {
            if(this.solution[customer] < 0) continue;

            costs += distances_facilitiesToCustomers[this.solution[customer]][customer];
            facilitesToBeConstructed.add(this.solution[customer]);
        }
        costs *= this.distanceCosts;

        for(int facility : facilitesToBeConstructed)
        {
            costs += this.calcConstructionCostsFor(facility);
        }

        return costs;
    }



    private int calcConstructionCostsFor(int facility)
    {
        int firstNum = this.baseOpeningCosts[facility];
        int secondNum = (int) (Math.ceil(1.5 * firstNum));
        int expansionLevel = this.calcExpansionLevelFor(facility);


        if(expansionLevel <= 0) return 0;
        else if(expansionLevel == 1) return firstNum;
        else if(expansionLevel == 2) return secondNum;

        for(int k = 3; k <= expansionLevel; k++)
        {
            int sum = firstNum + secondNum + (4 - k) * this.baseOpeningCosts[facility];
            firstNum = secondNum;
            secondNum = sum;
        }

        return secondNum;
    }



    private int calcExpansionLevelFor(int facility)        // updated 10.06.17
    {
        int neededBandwidth = 0;


        for(int customer = 0; customer < this.numOfCustomers; customer++)
        {
            if(this.solution[customer] == facility)
                neededBandwidth += this.demandedBandwidths[customer];
        }
        int level = neededBandwidth / this.maxBandwidths[facility];

        return (neededBandwidth % this.maxBandwidths[facility]) > 0 ? (level + 1) : level;
    }
}
