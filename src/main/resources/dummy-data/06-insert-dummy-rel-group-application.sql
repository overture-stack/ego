TRUNCATE public.groupapplication CASCADE;

-- Add all Approved groups to Perssonal Information Manager
INSERT INTO public.groupapplication (grpname, appname)
  SELECT g.grpname, a.appname
  FROM public.egogroup AS g
    LEFT JOIN public.egoapplication AS a
    ON a.appname='Personal Information Manager'
  WHERE g.grpname IN ('Pediatric Patient Support Network', 'Extreme Research Consortium', 'XYZ Cancer Research Institute', 'Healthcare Providers Anonymous');

-- Add Research Groups to the Data Portal
INSERT INTO public.groupapplication (grpname, appname)
  SELECT g.grpname, a.appname
  FROM public.egogroup AS g
    LEFT JOIN public.egoapplication AS a
      ON a.appname='Example Data Portal'
  WHERE g.grpname IN ('XYZ Cancer Research Institute', 'Extreme Research Consortium');