package fr.openmc.core.features.earth.models;
public enum EarthDiplomaticStatus { ALLIED(1.10D), NEUTRAL(1D), TENSE(0.94D), SANCTIONED(0.82D); private final double tradeMultiplier; EarthDiplomaticStatus(double tradeMultiplier){this.tradeMultiplier=tradeMultiplier;} public double tradeMultiplier(){return tradeMultiplier;} }
