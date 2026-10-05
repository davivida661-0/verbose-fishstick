package com.solarclient.bedwars;

import java.util.ArrayList;
import java.util.List;

/**
 * Dados da partida lidos da sidebar do servidor (BedWars).
 *
 * <p>Todos os campos sao "best effort": se o servidor mudar o formato da
 * sidebar, o valor simplesmente fica em -1 e o overlay esconde aquela
 * informacao. Nada aqui trava o jogo.</p>
 */
public final class BedWarsInfo {

    /** Uma equipe vista na sidebar. */
    public static final class Team {
        public String name = "?";
        public int players;
        public int beds = -1;
        public boolean eliminated;
        public boolean self;
    }

    public boolean inBedWars;
    public int iron = -1;
    public int gold = -1;
    public int diamond = -1;
    public int emerald = -1;
    public String forgeTier = "";
    public final List<Team> teams = new ArrayList<>();

    public void clear() {
        inBedWars = false;
        iron = gold = diamond = emerald = -1;
        forgeTier = "";
        teams.clear();
    }

    public int resource(int index) {
        switch (index) {
            case 0:
                return iron;
            case 1:
                return gold;
            case 2:
                return diamond;
            case 3:
                return emerald;
            default:
                return -1;
        }
    }
}
