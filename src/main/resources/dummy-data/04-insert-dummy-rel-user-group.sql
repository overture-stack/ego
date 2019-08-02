TRUNCATE public.usergroup CASCADE;

-- 4 Users to XYZ Research
INSERT INTO public.usergroup (user_id, group_id)
  SELECT u.id, g.id
  FROM public.egouser AS u
    LEFT JOIN public.egogroup AS g
      ON g.name='XYZ Cancer Research Institute'
  WHERE u.name IN ('Carmel.Corkery@example.com','Viviane.Langworth@example.com','Justice.Heller@example.com','Louvenia.Emard@example.com');

-- 19 Users into Extreme Research
INSERT INTO public.usergroup (user_id, group_id)
  SELECT u.id, g.id
  FROM public.egouser AS u
    LEFT JOIN public.egogroup AS g
      ON g.name='Extreme Research Consortium'
  WHERE u.name IN ('Elisha.Weimann@example.com','Kavon.Flatley@example.com','Carmel.Corkery@example.com','Keeley.Conn@example.com','Noe.Breitenberg@example.com','Lois.Ward@example.com','Brent.Brekke@example.com','Ruthe.Labadie@example.com','Lurline.Little@example.com','Justice.Heller@example.com','Ollie.Stroman@example.com','Devan.Harvey@example.com','Lupe.Hilll@example.com','Eudora.MacGyver@example.com','Elwyn.Reinger@example.com','Mara.Fisher@example.com','Romaine.Herman@example.com','Ali.Medhurst@example.com','Cali.Grimes@example.com');

-- 48 Users into Pediatric Patient Support Network
INSERT INTO public.usergroup (user_id, group_id)
  SELECT u.id, g.id
  FROM public.egouser AS u
    LEFT JOIN public.egogroup AS g
      ON g.name='Pediatric Patient Support Network'
  WHERE u.name IN ('Helen.Trantow@example.com','Waylon.Wiza@example.com','Yesenia.Schmeler@example.com','Furman.Volkman@example.com','Jeromy.Abernathy@example.com','Maya.DuBuque@example.com','Kenton.Kilback@example.com','Gordon.Ullrich@example.com','Lyda.Macejkovic@example.com','Marquis.Oberbrunner@example.com','Jeromy.Larkin@example.com','Ed.Olson@example.com','Hollie.Kunde@example.com','Jocelyn.Grant@example.com','Lupe.Hilll@example.com','Oral.Gleason@example.com','Chauncey.Schiller@example.com','Halie.Heller@example.com','Osvaldo.Bahringer@example.com','Daija.Pacocha@example.com','Gretchen.Wintheiser@example.com','Shayne.Lubowitz@example.com','Orlo.Mayer@example.com','Dorcas.Pfeffer@example.com','Justice.Heller@example.com','Domingo.Hoppe@example.com','Gideon.Klocko@example.com','Toney.Powlowski@example.com','Terry.Kuvalis@example.com','Cecilia.Rohan@example.com','Cali.Grimes@example.com','Maya.Upton@example.com','Kavon.Flatley@example.com','Art.Klein@example.com','Cornelius.Crona@example.com','Devan.Harvey@example.com','Tanya.Barton@example.com','Mireille.Bergstrom@example.com','Dakota.West@example.com','Arielle.Spinka@example.com','Noe.Breitenberg@example.com','Genesis.Lindgren@example.com','Neoma.Keeling@example.com','Merle.Jacobi@example.com','Hyman.Heathcote@example.com','Duane.Wiza@example.com','Esther.Labadie@example.com','Hilton.Kassulke@example.com');

-- 0 Users into Healthcare Providers Anonymous