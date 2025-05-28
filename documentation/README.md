# Specificații de Sistem pentru Platforma de Adopție Animale de Companie

## Abstract

Acest document prezintă specificațiile tehnice și funcționale pentru o aplicație web de adopție a animalelor de companie. Sistemul permite utilizatorilor autentificați să publice anunțuri pentru animale disponibile pentru adopție și să adopte animale, oferind funcționalități complete pentru gestionarea resurselor privitoare la îngrijirea acestora. Documentul detaliază cerințele funcționale și non-funcționale ale sistemului, arhitectura și modelul de date, precum și interacțiunea cu utilizatorul.

---

## 1. Introducere

### 1.1 Scop

Scopul acestui document este de a defini specificațiile pentru o platformă web de adopție animale de companie care să faciliteze conectarea familiilor cu animale disponibile pentru adopție și să ofere instrumente pentru gestionarea îngrijirii animalelor.

### 1.2 Publicul țintă

Acest document se adresează dezvoltatorilor, designerilor, testerilor și stakeholderilor implicați în dezvoltarea platformei, precum și potențialilor utilizatori și administratori ai sistemului.

### 1.3 Definirea problemei

În contextul actual, procesul de adopție a animalelor de companie este adesea fragmentat, fără o infrastructură digitală unificată care să permită gestionarea eficientă a informațiilor privind adopția, îngrijirea și evoluția animalelor. Există nevoia unei platforme digitale care să centralizeze aceste procese și să faciliteze comunicarea între adoptatori și persoanele care oferă animale spre adopție.

---

## 2. Descriere Generală

### 2.1 Perspectiva produsului

Platforma de Adopție a Animalelor de Companie este o aplicație web independentă care permite utilizatorilor autentificați să publice anunțuri pentru animale disponibile pentru adopție sau să adopte animale de companie. Sistemul interacționează cu o bază de date Oracle pentru stocarea datelor și oferă o interfață web accesibilă pentru utilizatori.

### 2.2 Funcționalitățile produsului

Principalele funcționalități ale platformei includ:

- **Autentificare și înregistrare:** Sistem complet pentru crearea și gestionarea conturilor de utilizator
- **Publicare anunțuri:** Crearea și gestionarea anunțurilor pentru animale disponibile
- **Căutare și filtrare:** Sistem avansat pentru găsirea animalelor potrivite criteriilor
- **Profil animal:** Gestionarea informațiilor și resurselor pentru fiecare animal
- **Calendar hrănire:** Programare și monitorizare a orarului de hrănire
- **Istoric medical:** Evidența vizitelor medicale, vaccinărilor și tratamentelor
- **Resurse multimedia:** Încărcarea și vizualizarea de fotografii și videoclipuri
- **Newsletter și RSS:** Notificări și flux de știri pentru oferte recente

### 2.3 Caracteristicile utilizatorilor

| Categorie utilizator      | Caracteristici                                 | Responsabilități                                                        |
|--------------------------|------------------------------------------------|-------------------------------------------------------------------------|
| Utilizatori neînregistrați | Persoane care vizitează platforma fără cont   | Pot vizualiza informații generale, însă nu pot interacționa cu funcționalitățile complete |
| Utilizatori înregistrați | Persoane care și-au creat cont pe platformă    | Pot publica anunțuri, adopta animale, gestiona profiluri de animale      |
| Administratori           | Personal cu drepturi speciale                  | Gestionează utilizatorii, validează anunțurile, moderează conținutul     |

### 2.4 Restricții și dependențe

- Aplicația necesită conexiune la internet
- Sistemul depinde de o bază de date Oracle
- Tehnologii necesare: Node.js v14+ și Oracle Instant Client
- Browser web modern pentru accesarea interfeței de utilizator
- Securitate pentru protejarea datelor personale ale utilizatorilor

---

## 3. Cerințe Specifice

### 3.1 Cerințe funcționale

- **FR1:** Autentificare și înregistrare utilizatori (cu email, parolă, nume, prenume, telefon, sesiuni securizate JWT)
- **FR2:** Publicare anunțuri pentru adopție (informații animal, fotografii, program hrănire, istoric medical, relații cu alte animale)
- **FR3:** Calendar hrănire (gestionare program hrănire, tip hrană, notițe)
- **FR4:** Istoric medical (vizite medicale, vaccinuri, tratamente, instrucțiuni prim ajutor)
- **FR5:** Încărcare și gestionare resurse multimedia (fotografii, videoclipuri, audio, descrieri)
- **FR6:** Filtrare și căutare (specie, rasă, vârstă, locație, alte criterii)
- **FR7:** Newsletter (abonare, preferințe, notificări)
- **FR8:** Flux RSS (filtrare după zone, rase, familii)

### 3.2 Cerințe de interfață

- Design responsiv pentru compatibilitate cu dispozitive mobile
- Navigare simplă și consistentă
- Formulare cu validare și feedback pentru utilizator
- Afișare clară a informațiilor despre animale
- Elemente vizuale atractive și prietenoase (culori calde, fonturi lizibile)

#### Interfețe hardware

- Computere desktop și laptop
- Tablete
- Telefoane mobile

#### Interfețe software

- Browsere web moderne (Chrome, Firefox, Safari, Edge)
- Sistem de bază de date Oracle

### 3.3 Cerințe de performanță

- Timpul de încărcare a paginilor sub 3 secunde
- Suport pentru cel puțin 1000 de utilizatori concurenți
- Timp de răspuns pentru operațiuni de bază de date sub 1 secundă
- Optimizarea resurselor multimedia pentru încărcare rapidă

### 3.4 Cerințe de design

| Tip               | Hex      | Utilizare                                 |
|-------------------|----------|-------------------------------------------|
| Culoare Primară   | #fca311  | Butoane, accente, elemente interactive    |
| Culoare Secundară | #14213d  | Headere, fundal meniu                     |
| Accent            | #e5e5e5  | Fundal, separatoare                       |
| Text              | #000000  | Conținut textual                          |

### 3.5 Cerințe de securitate

- Autentificare securizată utilizând JWT (JSON Web Tokens)
- Criptarea parolelor utilizatorilor
- Protecție împotriva atacurilor de tip SQL Injection
- Protecție împotriva atacurilor de tip Cross-Site Scripting (XSS)
- Controlul accesului bazat pe roluri
- Sesiuni securizate și expirare automată

---

## 4. Modelul Datelor

### 4.1 Entități principale

- **Users:** Stochează informații despre utilizatorii platformei
- **Animal:** Conține datele animalelor disponibile pentru adopție
- **FeedingSchedule:** Gestionează programul de hrănire pentru fiecare animal
- **MedicalHistory:** Stochează istoricul medical al animalelor
- **MultiMedia:** Gestionează resursele multimedia asociate animalelor
- **Address:** Stochează adresele utilizatorilor
- **Admins:** Informații despre administratorii platformei

### 4.2 Relații între entități

- Un **User** poate publica multiple **Animal**e
- Un **User** are o singură **Address**
- Un **Animal** are un singur **FeedingSchedule**
- Un **Animal** poate avea multiple înregistrări **MedicalHistory**
- Un **Animal** poate avea multiple resurse **MultiMedia**

---

## 5. Arhitectura Sistemului

### 5.1 Componente arhitecturale

- **Frontend:** HTML5, CSS3, JavaScript
- **Backend:** Node.js cu HTTP nativ
- **Baza de date:** Oracle Database

### 5.2 Fluxul de date

1. Utilizatorul interacționează cu interfața frontend
2. Cererea este trimisă către serverul backend
3. Backend-ul procesează cererea, interacționează cu baza de date și aplică logica de business
4. Rezultatul este returnat către frontend și afișat utilizatorului

---

## 6. Interacțiunea cu Utilizatorul

### 6.1 Principii de design UX

- **Simplitate:** Interfețe simple și intuitive
- **Consistență:** Elemente de design și interacțiune consistente în întreaga aplicație
- **Feedback:** Feedback vizual pentru acțiunile utilizatorului
- **Accesibilitate:** Design care respectă principiile de accesibilitate web

### 6.2 Scenarii de utilizare principale

#### Scenariul 1: Înregistrare și autentificare

1. Utilizatorul accesează pagina de înregistrare
2. Completează formularul cu datele personale (nume, prenume, email, parolă, telefon)
3. Trimite formularul și primește confirmare
4. Accesează pagina de autentificare
5. Introduce email și parolă pentru autentificare
6. Este redirecționat către pagina principală

#### Scenariul 2: Publicarea unui anunț de adopție

1. Utilizatorul accesează secțiunea "Publică Animal"
2. Completează informațiile de bază (nume, specie, rasă, vârstă, gen)
3. Încarcă o fotografie principală a animalului
4. Adaugă programul de hrănire
5. Completează istoricul medical și instrucțiuni de prim ajutor
6. Adaugă resurse multimedia suplimentare (fotografii, videoclipuri)
7. Specifică relații cu alte animale
8. Trimite formularul pentru publicare

#### Scenariul 3: Căutarea și filtrarea animalelor

1. Utilizatorul accesează pagina de căutare
2. Selectează criteriile de filtrare (specie, rasă, vârstă, locație)
3. Sistemul afișează rezultatele care corespund criteriilor
4. Utilizatorul poate sorta rezultatele după diverse criterii
5. Selectează un animal pentru a vizualiza detaliile complete

---

## 7. Concluzii

Platforma de Adopție a Animalelor de Companie oferă o soluție completă pentru facilitarea procesului de adopție și gestionarea informațiilor privind îngrijirea animalelor. Prin implementarea cerințelor specificate în acest document, sistemul va asigura o experiență intuitivă și eficientă pentru utilizatori, contribuind la îmbunătățirea procesului de adopție și îngrijire a animalelor de companie.

Dezvoltarea continuă a platformei va lua în considerare feedback-ul utilizatorilor și tendințele în domeniu pentru a îmbunătăți constant funcționalitățile și experiența utilizator.

---

## Referințe

1. IEEE. (1998). IEEE Recommended Practice for Software Requirements Specifications (IEEE Std 830-1998).
2. Oracle Corporation. (2023). Oracle Database Documentation.
3. Node.js Foundation. (2023). Node.js Documentation.
4. W3C. (2023). HTML5 Specification.
5. W3C. (2023). CSS3 Specification.