TRUNCATE public.groupapplication CASCADE;

-- Add all Approved groups to Perssonal Information Manager
INSERT INTO public.groupapplication (grpId, appId)
  SELECT g.id, a.id
  FROM public.egogroup AS g
    LEFT JOIN public.egoapplication AS a
    ON a.name='Personal Information Manager'
  WHERE g.name IN ('Pediatric Patient Support Network', 'Extreme Research Consortium', 'XYZ Cancer Research Institute', 'Healthcare Providers Anonymous');

-- Add Research Groups to the Data Portal
INSERT INTO public.groupapplication (grpId, appId)
  SELECT g.id, a.id
  FROM public.egogroup AS g
    LEFT JOIN public.egoapplication AS a
      ON a.name='Example Data Portal'
  WHERE g.name IN ('XYZ Cancer Research Institute', 'Extreme Research Consortium');