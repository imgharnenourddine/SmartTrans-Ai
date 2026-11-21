# Estimation des coûts AWS — SmartTrans-AI (dev)

Date des prix : 21 novembre 2026.
Région : `eu-west-3` (EU Paris), sauf le certificat CloudFront en `us-east-1`.
Source : API officielle AWS Price List, tarifs On-Demand, sans remise.
Change : 1 USD = 9,961938 MAD, relevé le 20 novembre 2026 via [ExchangeRate-API](https://open.er-api.com/v6/latest/USD).

Les montants n’incluent ni crédits, ni Savings Plans, ni remise de volume. Le Free Tier n’est déduit que lorsqu’il est indiqué comme conditionnel.

Durées utilisées : 168 heures pour une semaine et 730 heures pour un mois. Les frais mensuels fixes (stockage, secrets, zones DNS) sont ramenés à la semaine par le ratio 168/730.

## Architecture facturée

- Une instance EC2 `t3.small` et un volume racine gp3 de 30 Gio.
- Une passerelle NAT et son adresse IPv4 publique.
- Un Application Load Balancer sur deux zones de disponibilité.
- Une instance RDS PostgreSQL `db.t4g.micro`, mono-AZ, avec 20 Gio gp3.
- Trois secrets Secrets Manager.
- Deux zones Route 53 : la zone publique `smarttrans-ai.app` et la zone privée Cloud Map `smarttrans.local`.
- Une distribution CloudFront Price Class 100, sans AWS WAF et sans journaux d’accès.
- Deux dépôts ECR et les journaux CloudWatch des tâches ECS.

Les certificats ACM ne sont pas facturés. Aucun WAF n’est configuré.

## Coûts fixes, runtime démarré

Ces postes courent tant que les ressources existent, indépendamment du trafic.

| Service | Base de prix | Semaine (USD) | Mois (USD) |
|---|---:|---:|---:|
| EC2 `t3.small` | 0,0236 USD/h | 3,96 | 17,23 |
| EBS gp3 30 Gio | 0,0928 USD/Gio-mois | 0,64 | 2,78 |
| NAT Gateway | 0,05 USD/h | 8,40 | 36,50 |
| IPv4 publiques en service (3) | 0,005 USD/h chacune | 2,52 | 10,95 |
| Application Load Balancer | 0,02646 USD/h | 4,45 | 19,32 |
| RDS `db.t4g.micro` | 0,018 USD/h | 3,02 | 13,14 |
| Stockage RDS gp3 20 Gio | 0,133 USD/Gio-mois | 0,61 | 2,66 |
| Secrets Manager (3 secrets) | 0,40 USD/secret/mois | 0,28 | 1,20 |
| Route 53 (2 zones hébergées) | 0,50 USD/zone/mois | 0,23 | 1,00 |
| ECR, hypothèse 1 Gio | 0,10 USD/Gio-mois | 0,02 | 0,10 |
| **Total fixe** |  | **24,14** | **104,88** |

En dirhams, au taux ci-dessus : **240,44 MAD par semaine** et **1 044,79 MAD par mois**.

Les trois adresses IPv4 sont l’adresse de la NAT et les deux adresses de l’ALB, une par sous-réseau public. Les requêtes alias Route 53 vers CloudFront ne sont pas facturées.

## Coûts liés à l’usage

Ces postes dépendent du trafic. L’hypothèse de démonstration, pour un mois, est la suivante :

- 5 Gio traités par la NAT ;
- 0,25 LCU en moyenne sur l’ALB ;
- 50 000 requêtes HTTPS CloudFront depuis l’Europe ;
- 10 Gio sortants CloudFront vers l’Europe ;
- 1 Gio de journaux ECS ingérés ;
- aucun transfert Internet direct depuis l’ALB, les utilisateurs passant par CloudFront.

| Service | Prix unitaire | Mois (USD) |
|---|---:|---:|
| Données NAT | 0,05 USD/Gio | 0,25 |
| LCU de l’ALB | 0,0084 USD/LCU-heure | 1,53 |
| Requêtes HTTPS CloudFront | 0,012 USD / 10 000 | 0,06 |
| Sortie CloudFront, Europe | 0,085 USD/Gio | 0,85 |
| Ingestion CloudWatch Logs | 0,5985 USD/Gio | 0,60 |
| **Total d’usage hypothétique** |  | **3,29** |

La part hebdomadaire de cette hypothèse est de **0,76 USD**. Le transfert depuis une origine AWS vers CloudFront n’est pas facturé. Le premier palier CloudFront couvre les 10 premiers To.

## Total avec l’hypothèse de démonstration

| Période | Fixes | Usage | Total USD | Total MAD |
|---|---:|---:|---:|---:|
| 168 heures | 24,14 | 0,76 | 24,90 | 247,99 |
| 730 heures | 104,88 | 3,29 | 108,17 | 1 077,57 |

Un trafic plus élevé augmente surtout les LCU, la sortie CloudFront et les données traitées par la NAT. Il ne change pas les lignes fixes.

## Runtime arrêté

`scripts/aws-stop.sh` met les services ECS à zéro, retire la capacité EC2 et arrête RDS. Il ne supprime ni le VPC, ni la NAT, ni l’ALB, ni les données.

| Poste | Pendant l’arrêt |
|---|---|
| EC2 et volume racine | arrêtés : le volume est supprimé avec l’instance |
| Heures d’instance RDS | arrêtées |
| Stockage RDS 20 Gio | facturé : 2,66 USD/mois |
| NAT, ALB, trois IPv4, secrets, Route 53, ECR, CloudFront | facturés comme ci-dessus |

Le fixe restant est d’environ **71,73 USD/mois** (714,51 MAD), plus l’usage résiduel. AWS peut redémarrer automatiquement une instance RDS arrêtée au bout de sept jours.

## Free Tier

Le compte refuse les types d’instance qui ne sont pas marqués éligibles. `t3.small` est éligible à cette contrainte, mais son prix On-Demand reste 0,0236 USD/h. Cette éligibilité ne rend pas l’instance gratuite.

Si le compte est encore dans les douze mois du Free Tier classique, ces éléments peuvent être couverts : 750 heures et 20 Gio de `db.t4g.micro`, 30 Gio d’EBS, 100 Gio de transfert sortant et 5 Gio d’ingestion CloudWatch. Cette estimation ne déduit aucun de ces avantages, car la période restante n’a pas été vérifiée.

## Hors périmètre

Aucun crédit, aucune remise entreprise et aucun changement d’architecture destiné à réduire la facture ne sont inclus. Les appels API Secrets Manager et les requêtes DNS non alias restent négligeables au volume de démonstration.
