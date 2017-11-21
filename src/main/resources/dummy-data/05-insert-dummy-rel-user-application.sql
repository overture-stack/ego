TRUNCATE public.userapplication CASCADE;

-- 6 Users to Personal Information Manager
INSERT INTO public.userapplication (username, appname)
  SELECT u.username, a.appname
  FROM public.egouser AS u
    LEFT JOIN public.egoapplication AS a
      ON a.appname='Personal Information Manager'
  WHERE u.username IN ('Brennan.Denesik@example.com','Anika.Stehr@example.com','Janessa.Cronin@example.com','Sharon.Farrell@example.com','Zane.Rath@example.com','Elisha.Weimann@example.com');

-- 35 Users to Example Data Portal
INSERT INTO public.userapplication (username, appname)
  SELECT u.username, a.appname
  FROM public.egouser AS u
    LEFT JOIN public.egoapplication AS a
      ON a.appname='Example Data Portal'
  WHERE u.username IN ('Justice.Heller@example.com','Sharon.Farrell@example.com','Janessa.Cronin@example.com','Shayne.Lubowitz@example.com','Gretchen.Wintheiser@example.com','Daija.Pacocha@example.com','Osvaldo.Bahringer@example.com','Halie.Heller@example.com','Chauncey.Schiller@example.com','Oral.Gleason@example.com','Lupe.Hilll@example.com','Jocelyn.Grant@example.com','Hollie.Kunde@example.com','Ed.Olson@example.com','Jeromy.Larkin@example.com','Marquis.Oberbrunner@example.com','Lyda.Macejkovic@example.com','Gordon.Ullrich@example.com','Kenton.Kilback@example.com','Maya.DuBuque@example.com','Jeromy.Abernathy@example.com','Furman.Volkman@example.com','Yesenia.Schmeler@example.com','Waylon.Wiza@example.com','Helen.Trantow@example.com','Claudine.McKenzie@example.com','Korbin.Sawayn@example.com','Brionna.Mertz@example.com','Orin.Mraz@example.com','Rusty.Hickle@example.com','Rafaela.Harvey@example.com','Herminio.Kub@example.com','Lera.White@example.com','Chandler.Collier@example.com','Edd.Thompson@example.com');